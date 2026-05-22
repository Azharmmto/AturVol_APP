package com.example.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VolumeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VolumeViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSidebarOn by viewModel.isSidebarEnabled.collectAsState()
    val activeThemeName by viewModel.theme.collectAsState()

    // Real-time volumes
    val mediaVol by viewModel.volumeMedia.collectAsState()
    val ringVol by viewModel.volumeRing.collectAsState()
    val notifVol by viewModel.volumeNotification.collectAsState()
    val alarmVol by viewModel.volumeAlarm.collectAsState()
    val sysVol by viewModel.volumeSystem.collectAsState()
    val callVol by viewModel.volumeCall.collectAsState()

    // Determine Theme Colors dynamically
    val themeGradient = remember(activeThemeName) {
        when (activeThemeName) {
            "ELECTRIC_BLUE" -> Brush.verticalGradient(listOf(Color(0xFF030712), Color(0xFF0B1528)))
            "BLACK_PREMIUM" -> Brush.verticalGradient(listOf(Color(0xFF090909), Color(0xFF141414)))
            "CHARCOAL" -> Brush.verticalGradient(listOf(Color(0xFF111215), Color(0xFF1C1D21)))
            else -> Brush.verticalGradient(listOf(listOf(Color(0xFF070B19), Color(0xFF0F172A)).first(), listOf(Color(0xFF070B19), Color(0xFF0F172A)).last())) // DARK_NAVY
        }
    }

    val accentColor = remember(activeThemeName) {
        when (activeThemeName) {
            "ELECTRIC_BLUE" -> Color(0xFF00F0FF)
            "BLACK_PREMIUM" -> Color(0xFFE2E8F0)
            "CHARCOAL" -> Color(0xFFFF5A5F)
            else -> Color(0xFF4CC9F0) // DARK_NAVY
        }
    }

    val secondaryAccent = remember(activeThemeName) {
        when (activeThemeName) {
            "ELECTRIC_BLUE" -> Color(0xFF3B82F6)
            "BLACK_PREMIUM" -> Color(0xFF71717A)
            "CHARCOAL" -> Color(0xFFFFA200)
            else -> Color(0xFF3F37C9) // DARK_NAVY
        }
    }

    // Checking overlay settings permission
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Check again when the screen comes to foreground
    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        // Auto-refresh service state if enabled
        if (isSidebarOn && hasOverlayPermission) {
            viewModel.updateServiceState(true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Screen Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AturVol Dashboard",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Atur volume sistem & panel melayang",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Buka Pengaturan",
                        tint = accentColor
                    )
                }
            }

            // Quick Banner alerting Overlay Permission triggers
            if (!hasOverlayPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x26EF4444)),
                    border = BorderStroke(1.dp, Color(0x66EF4444))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Pemberitahuan",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Izin Ditangguhkan",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Aktifkan 'Tampilkan di atas aplikasi lain' agar Panel VoluEdge bekerja.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Izinkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sidebar Control Box Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Launch,
                                        contentDescription = "Sidebar Toggle Status",
                                        tint = accentColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Sidebar Volume",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isSidebarOn) "Layanan Aktif (Geser tepi layar)" else "Layanan Tidak Aktif",
                                        fontSize = 11.sp,
                                        color = if (isSidebarOn) accentColor else Color.White.copy(alpha = 0.5f),
                                        fontWeight = if (isSidebarOn) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            Switch(
                                checked = isSidebarOn,
                                onCheckedChange = { checked ->
                                    if (checked && !Settings.canDrawOverlays(context)) {
                                        // Request Permission
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        viewModel.toggleSidebar(checked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accentColor,
                                    checkedTrackColor = accentColor.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag("toggle_sidebar_switch")
                            )
                        }
                    }
                }

                // Volume Controls Hub Section Title
                Text(
                    text = "Kontrol Volume Saat Ini",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                // Render lists of sliders inside modern glowing container boxes
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    VolumeControllerCard(
                        title = "Media Player",
                        subtitle = "Audio game, video, musik",
                        icon = if (mediaVol == 0) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp,
                        value = mediaVol,
                        max = viewModel.maxMedia,
                        accentColor = accentColor,
                        onValueChange = { viewModel.setVolume(AudioManager.STREAM_MUSIC, it) }
                    )

                    VolumeControllerCard(
                        title = "Ringtone Panggilan",
                        subtitle = "Panggilan masuk seluler",
                        icon = if (ringVol == 0) Icons.Rounded.VolumeOff else Icons.Rounded.NotificationsActive,
                        value = ringVol,
                        max = viewModel.maxRing,
                        accentColor = accentColor,
                        onValueChange = { viewModel.setVolume(AudioManager.STREAM_RING, it) }
                    )

                    VolumeControllerCard(
                        title = "Nada Notifikasi",
                        subtitle = "Pemberitahuan chat & aplikasi",
                        icon = if (notifVol == 0) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                        value = notifVol,
                        max = viewModel.maxNotification,
                        accentColor = accentColor,
                        onValueChange = { viewModel.setVolume(AudioManager.STREAM_NOTIFICATION, it) }
                    )

                    VolumeControllerCard(
                        title = "Jam Alarm dsb",
                        subtitle = "Pengingat, alarm pagi",
                        icon = Icons.Rounded.AccessTime,
                        value = alarmVol,
                        max = viewModel.maxAlarm,
                        accentColor = accentColor,
                        onValueChange = { viewModel.setVolume(AudioManager.STREAM_ALARM, it) }
                    )

                    VolumeControllerCard(
                        title = "Volume Sistem",
                        subtitle = "Suara interface & click internal",
                        icon = Icons.Rounded.SettingsSystemDaydream,
                        value = sysVol,
                        max = viewModel.maxSystem,
                        accentColor = accentColor,
                        onValueChange = { viewModel.setVolume(AudioManager.STREAM_SYSTEM, it) }
                    )

                    VolumeControllerCard(
                        title = "Volume Panggilan",
                        subtitle = "Suara pembicara telepon / headset",
                        icon = Icons.Rounded.Phone,
                        value = callVol,
                        max = viewModel.maxCall,
                        accentColor = accentColor,
                        onValueChange = { viewModel.setVolume(AudioManager.STREAM_VOICE_CALL, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun VolumeControllerCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    value: Int,
    max: Int,
    accentColor: Color,
    onValueChange: (Int) -> Unit
) {
    var sliderValue by remember(value) { mutableStateOf(value.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accentColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // Volume level badge
                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${sliderValue.toInt()} / $max",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slider with custom Material 3 styling
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
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                    thumbColor = accentColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_slider_${title.split(" ").first().lowercase()}")
            )
        }
    }
}
