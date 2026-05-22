package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.abs

class VolumeOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val CHANNEL_ID = "vol_edge_service_channel"
        const val NOTIFICATION_ID = 1337
        const val ACTION_START_SERVICE = "START_VOLUME_SERVICE"
        const val ACTION_STOP_SERVICE = "STOP_VOLUME_SERVICE"
        const val ACTION_UPDATE_SETTINGS = "UPDATE_VOLUME_SETTINGS"
    }

    // Lifecycle, ViewModel, and SavedState setups for Service ComposeView
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private lateinit var audioManager: AudioManager
    private lateinit var settings: AppSettings
    private var vibrator: Vibrator? = null

    // Compose overlay elements
    private var overlayComposeView: ComposeView? = null
    private var handleLayoutParams = WindowManager.LayoutParams()
    private var panelLayoutParams = WindowManager.LayoutParams()

    // Observable states representing setting configs
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var volumeObserver: ContentObserver? = null

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                if (System.currentTimeMillis() - lastUserSetVolumeTime > 1200L) {
                    updateVolumesFromSystem()
                }
            }
        }
    }

    // Overlay State Variables shared with Compose content
    private val isPanelExpanded = MutableStateFlow(false)
    private val volumeMediaValue = MutableStateFlow(0)
    private val volumeRingValue = MutableStateFlow(0)
    private val volumeNotifValue = MutableStateFlow(0)
    private val volumeAlarmValue = MutableStateFlow(0)
    private val volumeSysValue = MutableStateFlow(0)
    private val volumeCallValue = MutableStateFlow(0)

    private val maxVolumeMedia = MutableStateFlow(15)
    private val maxVolumeRing = MutableStateFlow(7)
    private val maxVolumeNotif = MutableStateFlow(7)
    private val maxVolumeAlarm = MutableStateFlow(7)
    private val maxVolumeSys = MutableStateFlow(7)
    private val maxVolumeCall = MutableStateFlow(5)

    private var savedHandleY = 400
    private var lastUserSetVolumeTime = 0L

    private fun setServiceStreamVolume(streamType: Int, value: Int) {
        val current = try {
            audioManager.getStreamVolume(streamType)
        } catch (e: Exception) {
            -1
        }
        if (current == value) {
            return
        }
        lastUserSetVolumeTime = System.currentTimeMillis()
        
        // Immediately update local StateFlows to keep UI perfectly synchronized and buttery smooth
        when (streamType) {
            AudioManager.STREAM_MUSIC -> volumeMediaValue.value = value
            AudioManager.STREAM_RING -> volumeRingValue.value = value
            AudioManager.STREAM_NOTIFICATION -> volumeNotifValue.value = value
            AudioManager.STREAM_ALARM -> volumeAlarmValue.value = value
            AudioManager.STREAM_SYSTEM -> volumeSysValue.value = value
            AudioManager.STREAM_VOICE_CALL -> volumeCallValue.value = value
        }

        // Apply to System AudioManager asynchronously on IO thread to prevent UI thread lock/blocking lag
        serviceScope.launch(Dispatchers.IO) {
            try {
                audioManager.setStreamVolume(streamType, value, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        settings = AppSettings(this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // Initialize volumes and maximums
        updateVolumesFromSystem()

        // Content observer for volume variations
        volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                if (System.currentTimeMillis() - lastUserSetVolumeTime > 1200L) {
                    updateVolumesFromSystem()
                }
            }
        }
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver!!
        )

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(volumeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(volumeReceiver, filter)
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground Notification first to satisfy Android requirements
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        if (action == ACTION_START_SERVICE || action == ACTION_UPDATE_SETTINGS) {
            initOrUpdateOverlay()
            // Ensure settings.sidebarEnabled is synced to true if service is actively running
            if (!settings.getSidebarEnabled()) {
                settings.setSidebarEnabled(true)
            }
        }

        return START_STICKY
    }

    private fun updateVolumesFromSystem() {
        volumeMediaValue.value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeRingValue.value = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        volumeNotifValue.value = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        volumeAlarmValue.value = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        volumeSysValue.value = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
        volumeCallValue.value = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)

        maxVolumeMedia.value = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        maxVolumeRing.value = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        maxVolumeNotif.value = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).coerceAtLeast(1)
        maxVolumeAlarm.value = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        maxVolumeSys.value = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM).coerceAtLeast(1)
        maxVolumeCall.value = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL).coerceAtLeast(1)
    }

    private fun triggerFeedback() {
        if (!settings.getVibrationEnabled()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initOrUpdateOverlay() {
        if (overlayComposeView != null) {
            try {
                windowManager.removeView(overlayComposeView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Setup Window Layout parameters of overlay handle initially
        val density = resources.displayMetrics.scaledDensity
        val barPosition = settings.getSidebarPosition()
        val gravityValue = if (barPosition == "LEFT") Gravity.START or Gravity.TOP else Gravity.END or Gravity.TOP

        handleLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = gravityValue
            x = 0
            y = getPreferencesHandleY()
        }

        // Setup compost view
        overlayComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VolumeOverlayService)
            setViewTreeViewModelStoreOwner(this@VolumeOverlayService)
            setViewTreeSavedStateRegistryOwner(this@VolumeOverlayService)
            setContent {
                VolumeOverlayContent()
            }
        }

        try {
            windowManager.addView(overlayComposeView, handleLayoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPreferencesHandleY(): Int {
        val sharedPrefs = getSharedPreferences("volege_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getInt("handle_y_pos", 400)
    }

    private fun savePreferencesHandleY(y: Int) {
        val sharedPrefs = getSharedPreferences("volege_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("handle_y_pos", y).apply()
    }

    private fun switchToExpandedParams() {
        panelLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        try {
            windowManager.updateViewLayout(overlayComposeView, panelLayoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun switchToCollapsedParams() {
        val barPosition = settings.getSidebarPosition()
        val gravityValue = if (barPosition == "LEFT") Gravity.START or Gravity.TOP else Gravity.END or Gravity.TOP

        handleLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = gravityValue
            x = 0
            y = getPreferencesHandleY()
        }
        try {
            windowManager.updateViewLayout(overlayComposeView, handleLayoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    private fun VolumeOverlayContent() {
        val isExpanded by isPanelExpanded.collectAsState()
        val posSetting by settings.sidebarPosition.collectAsState()
        val transSetting by settings.sidebarTransparency.collectAsState()
        val sizeSetting by settings.sidebarSize.collectAsState()
        val autoHideSec by settings.autoHideDuration.collectAsState()
        val currentThemeName by settings.theme.collectAsState()

        // Track auto-hide timer dynamically
        var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

        LaunchedEffect(isExpanded) {
            if (isExpanded) {
                while (true) {
                    delay(1000)
                    if (System.currentTimeMillis() - lastInteractionTime >= autoHideSec * 1000L) {
                        isPanelExpanded.value = false
                        switchToCollapsedParams()
                        break
                    }
                }
            }
        }

        // Color theme mapping
        val themeColors = remember(currentThemeName) {
            when (currentThemeName) {
                "ELECTRIC_BLUE" -> ThemePalette(
                    bg = Color(0xEC070F1E),
                    primary = Color(0xFF00F0FF),
                    secondary = Color(0xFF3B82F6),
                    text = Color.White
                )
                "BLACK_PREMIUM" -> ThemePalette(
                    bg = Color(0xF4080808),
                    primary = Color(0xFFE2E8F0),
                    secondary = Color(0xFFA1A1AA),
                    text = Color.White
                )
                "CHARCOAL" -> ThemePalette(
                    bg = Color(0xEE1E2022),
                    primary = Color(0xFFFF5A5F),
                    secondary = Color(0xFFFFB400),
                    text = Color.White
                )
                else -> ThemePalette( // DARK_NAVY
                    bg = Color(0xEE0B132B),
                    primary = Color(0xFF4CC9F0),
                    secondary = Color(0xFF3F37C9),
                    text = Color.White
                )
            }
        }

        val mediaVal by volumeMediaValue.collectAsState()
        val ringVal by volumeRingValue.collectAsState()
        val notifVal by volumeNotifValue.collectAsState()
        val alarmVal by volumeAlarmValue.collectAsState()
        val sysVal by volumeSysValue.collectAsState()
        val callVal by volumeCallValue.collectAsState()

        val maxMedia by maxVolumeMedia.collectAsState()
        val maxRing by maxVolumeRing.collectAsState()
        val maxNotif by maxVolumeNotif.collectAsState()
        val maxAlarm by maxVolumeAlarm.collectAsState()
        val maxSys by maxVolumeSys.collectAsState()
        val maxCall by maxVolumeCall.collectAsState()

        if (!isExpanded) {
            // Display Handle ONLY
            val animatedWidth by animateDpAsState(
                targetValue = when (sizeSetting) {
                    "SMALL" -> 10.dp
                    "LARGE" -> 22.dp
                    else -> 15.dp
                }, label = "handleWidth"
            )

            val animatedHeight by animateDpAsState(
                targetValue = when (sizeSetting) {
                    "SMALL" -> 70.dp
                    "LARGE" -> 150.dp
                    else -> 105.dp
                }, label = "handleHeight"
            )

            // Transparent handle touch handler
            Box(
                modifier = Modifier
                    .size(width = animatedWidth + 24.dp, height = animatedHeight + 30.dp)
                    .pointerInput(posSetting) {
                        detectDragGestures(
                            onDragStart = { },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                lastInteractionTime = System.currentTimeMillis()
                                handleLayoutParams.y = (handleLayoutParams.y + dragAmount.y.toInt()).coerceIn(100, 2500)
                                savePreferencesHandleY(handleLayoutParams.y)
                                try {
                                    windowManager.updateViewLayout(overlayComposeView, handleLayoutParams)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        triggerFeedback()
                        isPanelExpanded.value = true
                        lastInteractionTime = System.currentTimeMillis()
                        switchToExpandedParams()
                    },
                contentAlignment = Alignment.Center
            ) {
                // Colored small pill representing handle
                Box(
                    modifier = Modifier
                        .size(width = animatedWidth, height = animatedHeight)
                        .alpha(transSetting)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(themeColors.primary, themeColors.secondary)
                            ),
                            shape = if (posSetting == "LEFT") RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                            else RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                        )
                )
            }
        } else {
            // Center panel container that captures background clicks
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x3B000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPanelExpanded.value = false
                        switchToCollapsedParams()
                    },
                contentAlignment = if (posSetting == "LEFT") Alignment.CenterStart else Alignment.CenterEnd
            ) {
                var panelOffsetState by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    panelOffsetState = true
                }

                // Volume slider card panel container
                AnimatedVisibility(
                    visible = panelOffsetState,
                    enter = slideInHorizontally(
                        initialOffsetX = { if (posSetting == "LEFT") -it else it },
                        animationSpec = spring(stiffness = 350f)
                    ) + fadeIn(),
                    exit = slideOutHorizontally(
                        targetOffsetX = { if (posSetting == "LEFT") -it else it }
                    ) + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .padding(vertical = 40.dp)
                            .padding(
                                start = if (posSetting == "LEFT") 16.dp else 0.dp,
                                end = if (posSetting == "RIGHT") 16.dp else 0.dp
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // Prevent dismissing overlay when tapping volume card inside
                                lastInteractionTime = System.currentTimeMillis()
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.bg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Panel Top Header Title / Settings option shortcut
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Edge Volume",
                                    color = themeColors.text,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )

                                Row {
                                    // Settings Shortcut Button
                                    IconButton(
                                        onClick = {
                                            triggerFeedback()
                                            val mainIntent = Intent(this@VolumeOverlayService, MainActivity::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            }
                                            startActivity(mainIntent)
                                            isPanelExpanded.value = false
                                            switchToCollapsedParams()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = "Open App Settings",
                                            tint = themeColors.primary
                                        )
                                    }

                                    // Collapse Overlay Panel button
                                    IconButton(
                                        onClick = {
                                            triggerFeedback()
                                            isPanelExpanded.value = false
                                            switchToCollapsedParams()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Minimize Volume Panel",
                                            tint = themeColors.text.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            Divider(color = themeColors.text.copy(alpha = 0.15f), modifier = Modifier.padding(bottom = 16.dp))

                            // Scrollable volume list elements inside slider card container
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Media volume slider item widget
                                OverlaySliderItem(
                                    title = "Media",
                                    icon = if (mediaVal == 0) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp,
                                    value = mediaVal,
                                    max = maxMedia,
                                    accentColor = themeColors.primary,
                                    textColor = themeColors.text,
                                    onValueChange = { newVal ->
                                        lastInteractionTime = System.currentTimeMillis()
                                        setServiceStreamVolume(AudioManager.STREAM_MUSIC, newVal)
                                        triggerFeedback()
                                    }
                                )

                                // Ring volume slider item widget
                                OverlaySliderItem(
                                    title = "Ringtone",
                                    icon = if (ringVal == 0) Icons.Rounded.VolumeOff else Icons.Rounded.NotificationsActive,
                                    value = ringVal,
                                    max = maxRing,
                                    accentColor = themeColors.primary,
                                    textColor = themeColors.text,
                                    onValueChange = { newVal ->
                                        lastInteractionTime = System.currentTimeMillis()
                                        setServiceStreamVolume(AudioManager.STREAM_RING, newVal)
                                        triggerFeedback()
                                    }
                                )

                                // Notification volume slider item widget
                                OverlaySliderItem(
                                    title = "Notification",
                                    icon = if (notifVal == 0) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                                    value = notifVal,
                                    max = maxNotif,
                                    accentColor = themeColors.primary,
                                    textColor = themeColors.text,
                                    onValueChange = { newVal ->
                                        lastInteractionTime = System.currentTimeMillis()
                                        setServiceStreamVolume(AudioManager.STREAM_NOTIFICATION, newVal)
                                        triggerFeedback()
                                    }
                                )

                                // Alarm volume slider item widget
                                OverlaySliderItem(
                                    title = "Alarm",
                                    icon = Icons.Rounded.AccessTime,
                                    value = alarmVal,
                                    max = maxAlarm,
                                    accentColor = themeColors.primary,
                                    textColor = themeColors.text,
                                    onValueChange = { newVal ->
                                        lastInteractionTime = System.currentTimeMillis()
                                        setServiceStreamVolume(AudioManager.STREAM_ALARM, newVal)
                                        triggerFeedback()
                                    }
                                )

                                // System volume slider item widget
                                OverlaySliderItem(
                                    title = "System",
                                    icon = Icons.Rounded.SettingsSystemDaydream,
                                    value = sysVal,
                                    max = maxSys,
                                    accentColor = themeColors.primary,
                                    textColor = themeColors.text,
                                    onValueChange = { newVal ->
                                        lastInteractionTime = System.currentTimeMillis()
                                        setServiceStreamVolume(AudioManager.STREAM_SYSTEM, newVal)
                                        triggerFeedback()
                                    }
                                )

                                // Call volume slider item widget
                                OverlaySliderItem(
                                    title = "Voice Call",
                                    icon = Icons.Rounded.Phone,
                                    value = callVal,
                                    max = maxCall,
                                    accentColor = themeColors.primary,
                                    textColor = themeColors.text,
                                    onValueChange = { newVal ->
                                        lastInteractionTime = System.currentTimeMillis()
                                        setServiceStreamVolume(AudioManager.STREAM_VOICE_CALL, newVal)
                                        triggerFeedback()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun OverlaySliderItem(
        title: String,
        icon: ImageVector,
        value: Int,
        max: Int,
        accentColor: Color,
        textColor: Color,
        onValueChange: (Int) -> Unit
    ) {
        var sliderValue by remember(value) { mutableStateOf(value.toFloat()) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "$title Icon",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${sliderValue.toInt()} / $max",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = sliderValue,
                onValueChange = { newVal ->
                    sliderValue = newVal
                    onValueChange(newVal.toInt())
                },
                valueRange = 0f..max.toFloat(),
                steps = if (max > 1) max - 1 else 0,
                colors = SliderDefaults.colors(
                    activeTrackColor = accentColor,
                    inactiveTrackColor = textColor.copy(alpha = 0.15f),
                    thumbColor = accentColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overlay_slider_${title.lowercase()}")
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop observation
        volumeObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        try {
            unregisterReceiver(volumeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Cleanup Window Views
        if (overlayComposeView != null) {
            try {
                windowManager.removeView(overlayComposeView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayComposeView = null
        }
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        serviceScope.cancel()
        settings.cleanup()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Volume Sidebar Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background task maintaining the floating volume panel handle."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val disableIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VolumeOverlayService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_media_play) // fallback icon
            .setContentTitle("Volume Slider Aktif")
            .setContentText("Geser handle di sisi layar untuk atur volume")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Matikan Sidebar",
                disableIntent
            )
            .build()
    }

    data class ThemePalette(
        val bg: Color,
        val primary: Color,
        val secondary: Color,
        val text: Color
    )
}
