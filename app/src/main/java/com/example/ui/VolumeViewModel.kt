package com.example.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.service.VolumeOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class VolumeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val settings = AppSettings(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var lastUserSetVolumeTime = 0L

    // Real-time volumes
    private val _volumeMedia = MutableStateFlow(0)
    val volumeMedia = _volumeMedia.asStateFlow()

    private val _volumeRing = MutableStateFlow(0)
    val volumeRing = _volumeRing.asStateFlow()

    private val _volumeNotification = MutableStateFlow(0)
    val volumeNotification = _volumeNotification.asStateFlow()

    private val _volumeAlarm = MutableStateFlow(0)
    val volumeAlarm = _volumeAlarm.asStateFlow()

    private val _volumeSystem = MutableStateFlow(0)
    val volumeSystem = _volumeSystem.asStateFlow()

    private val _volumeCall = MutableStateFlow(0)
    val volumeCall = _volumeCall.asStateFlow()

    // Maximum volumes
    val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
    val maxNotification = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).coerceAtLeast(1)
    val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
    val maxSystem = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM).coerceAtLeast(1)
    val maxCall = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL).coerceAtLeast(1)

    // Reactive Setting States from DataStore/Prefs
    val isSidebarEnabled = settings.isSidebarEnabled
    val sidebarPosition = settings.sidebarPosition
    val sidebarTransparency = settings.sidebarTransparency
    val sidebarSize = settings.sidebarSize
    val autoHideDuration = settings.autoHideDuration
    val theme = settings.theme
    val vibrationEnabled = settings.vibrationEnabled
    val startupAtBoot = settings.startupAtBoot

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            if (System.currentTimeMillis() - lastUserSetVolumeTime > 1200L) {
                refreshVolumes()
            }
        }
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                if (System.currentTimeMillis() - lastUserSetVolumeTime > 1200L) {
                    refreshVolumes()
                }
            }
        }
    }

    init {
        refreshVolumes()
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver
        )
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(volumeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(volumeReceiver, filter)
        }
    }

    fun refreshVolumes() {
        _volumeMedia.value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        _volumeRing.value = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        _volumeNotification.value = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        _volumeAlarm.value = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        _volumeSystem.value = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
        _volumeCall.value = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
    }

    fun setVolume(streamType: Int, value: Int) {
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
            AudioManager.STREAM_MUSIC -> _volumeMedia.value = value
            AudioManager.STREAM_RING -> _volumeRing.value = value
            AudioManager.STREAM_NOTIFICATION -> _volumeNotification.value = value
            AudioManager.STREAM_ALARM -> _volumeAlarm.value = value
            AudioManager.STREAM_SYSTEM -> _volumeSystem.value = value
            AudioManager.STREAM_VOICE_CALL -> _volumeCall.value = value
        }

        // Apply to AudioManager asynchronously on IO Thread to prevent UI thread blocking/lagging
        viewModelScope.launch(Dispatchers.IO) {
            try {
                audioManager.setStreamVolume(streamType, value, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Service Management
    fun toggleSidebar(enabled: Boolean) {
        settings.setSidebarEnabled(enabled)
        updateServiceState(enabled)
    }

    fun updateServiceState(enabled: Boolean) {
        val intent = Intent(context, VolumeOverlayService::class.java)
        if (enabled) {
            intent.action = VolumeOverlayService.ACTION_START_SERVICE
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            intent.action = VolumeOverlayService.ACTION_STOP_SERVICE
            context.startService(intent)
        }
    }

    fun notifyServiceSettingsChanged() {
        if (settings.getSidebarEnabled()) {
            val intent = Intent(context, VolumeOverlayService::class.java).apply {
                action = VolumeOverlayService.ACTION_UPDATE_SETTINGS
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Setting modifiers called from Settings Screen
    fun setSidebarPosition(position: String) {
        settings.setSidebarPosition(position)
        notifyServiceSettingsChanged()
    }

    fun setSidebarTransparency(transparency: Float) {
        settings.setSidebarTransparency(transparency)
        notifyServiceSettingsChanged()
    }

    fun setSidebarSize(size: String) {
        settings.setSidebarSize(size)
        notifyServiceSettingsChanged()
    }

    fun setAutoHideDuration(duration: Int) {
        settings.setAutoHideDuration(duration)
        notifyServiceSettingsChanged()
    }

    fun setTheme(theme: String) {
        settings.setTheme(theme)
        notifyServiceSettingsChanged()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        settings.setVibrationEnabled(enabled)
    }

    fun setStartupAtBoot(enabled: Boolean) {
        settings.setStartupAtBoot(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        context.contentResolver.unregisterContentObserver(volumeObserver)
        try {
            context.unregisterReceiver(volumeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        settings.cleanup()
    }
}
