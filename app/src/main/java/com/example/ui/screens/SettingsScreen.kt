package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.core.content.ContextCompat
import com.example.ui.VolumeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VolumeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe Preference State structures from ViewModel
    val isSidebarOn by viewModel.isSidebarEnabled.collectAsState()
    val barPosition by viewModel.sidebarPosition.collectAsState()
    val transparency by viewModel.sidebarTransparency.collectAsState()
    val sizeSetting by viewModel.sidebarSize.collectAsState()
    val autoHideDuration by viewModel.autoHideDuration.collectAsState()
    val activeThemeName by viewModel.theme.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val startupAtBoot by viewModel.startupAtBoot.collectAsState()

    // Permissions check
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Checking system states on refresh entry
    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Permission launcher for Android 13+ Notification
    val requestNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Color theme specifications
    val themeGradient = remember(activeThemeName) {
        when (activeThemeName) {
            "ELECTRIC_BLUE" -> Brush.verticalGradient(listOf(Color(0xFF030712), Color(0xFF0D1220)))
            "BLACK_PREMIUM" -> Brush.verticalGradient(listOf(Color(0xFF090909), Color(0xFF141414)))
            "CHARCOAL" -> Brush.verticalGradient(listOf(Color(0xFF121316), Color(0xFF1F2025)))
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
            // Header panel row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Kembali ke Dashboard",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Pengaturan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Kostumisasi & manajemen fungsional",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Visual Design Options
                SettingsGroup(title = "Desain & Tata Letak", accentColor = accentColor) {
                    // Position Picker Options
                    SettingsSegmentedRow(
                        title = "Posisi Sidebar di Layar",
                        subtitle = "Kiri atau kanan tepian perangkat",
                        options = listOf("LEFT" to "Sisi Kiri", "RIGHT" to "Sisi Kanan"),
                        selectedValue = barPosition,
                        accentColor = accentColor,
                        onSelected = { viewModel.setSidebarPosition(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Size Picker options
                    SettingsSegmentedRow3(
                        title = "Tinggi / Ukuran Handle",
                        subtitle = "Seberapa panjang bidang sentuh melayang",
                        options = listOf("SMALL" to "Kecil", "MEDIUM" to "Sedang", "LARGE" to "Besar"),
                        selectedValue = sizeSetting,
                        accentColor = accentColor,
                        onSelected = { viewModel.setSidebarSize(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Transparency Slider row
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Transparansi Handle",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Mengurangi gangguan visual",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Text(
                                text = "${(transparency * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Slider(
                            value = transparency,
                            onValueChange = { viewModel.setSidebarTransparency(it) },
                            valueRange = 0.15f..1.0f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                                thumbColor = accentColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_transparency_slider")
                        )
                    }
                }

                // Section 2: Behavior Configurations
                SettingsGroup(title = "Perilaku Sidebar", accentColor = accentColor) {
                    // Auto-hide slider duration configuration
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Collapse Duration",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Durasi sembunyi otomatis panel volume",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Text(
                                text = "$autoHideDuration Detik",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Slider(
                            value = autoHideDuration.toFloat(),
                            onValueChange = { viewModel.setAutoHideDuration(it.toInt()) },
                            valueRange = 2f..10f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                                thumbColor = accentColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_duration_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Vibration feedbacks
                    SettingsToggleRow(
                        title = "Umpan Balik Vibrasi",
                        subtitle = "Getaran haptic halus saat menggeser volume",
                        icon = Icons.Rounded.Vibration,
                        checked = vibrationEnabled,
                        accentColor = accentColor,
                        onCheckedChange = { viewModel.setVibrationEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Launch on Device Boots
                    SettingsToggleRow(
                        title = "Mulai Otomatis saat Booting",
                        subtitle = "Aktifkan otomatis setelah hp dinyalakan",
                        icon = Icons.Rounded.PowerSettingsNew,
                        checked = startupAtBoot,
                        accentColor = accentColor,
                        onCheckedChange = { viewModel.setStartupAtBoot(it) }
                    )
                }

                // Section 3: Themes selectors
                SettingsGroup(title = "Tema Tampilan", accentColor = accentColor) {
                    Text(
                        text = "Pilih Preset Warna Solid Premium",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeOptionSelector(
                            name = "Dark Navy",
                            colorSymbol = Color(0xFF4CC9F0),
                            bgSample = Color(0xFF0F172A),
                            selected = activeThemeName == "DARK_NAVY",
                            onClick = { viewModel.setTheme("DARK_NAVY") },
                            modifier = Modifier.weight(1f)
                        )

                        ThemeOptionSelector(
                            name = "Electric Blue",
                            colorSymbol = Color(0xFF00F0FF),
                            bgSample = Color(0xFF0B1220),
                            selected = activeThemeName == "ELECTRIC_BLUE",
                            onClick = { viewModel.setTheme("ELECTRIC_BLUE") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeOptionSelector(
                            name = "Onyx Premium",
                            colorSymbol = Color(0xFFE2E8F0),
                            bgSample = Color(0xFF090909),
                            selected = activeThemeName == "BLACK_PREMIUM",
                            onClick = { viewModel.setTheme("BLACK_PREMIUM") },
                            modifier = Modifier.weight(1f)
                        )

                        ThemeOptionSelector(
                            name = "Charcoal Ruby",
                            colorSymbol = Color(0xFFFF5A5F),
                            bgSample = Color(0xFF1F2025),
                            selected = activeThemeName == "CHARCOAL",
                            onClick = { viewModel.setTheme("CHARCOAL") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Section 4: System Permissions Admin
                SettingsGroup(title = "Keamanan & Izin Sistem", accentColor = accentColor) {
                    PermissionRow(
                        title = "Izin 'Tampilkan di Atas Aplikasi'",
                        subtitle = "Wajib agar layout sidebar dapat digambar di background",
                        isGranted = hasOverlayPermission,
                        accentColor = accentColor,
                        onRequest = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PermissionRow(
                        title = "Izin Notifikasi (Android 13+)",
                        subtitle = "Wajib agar sistem task tidak dimatikan paksa di Android",
                        isGranted = hasNotificationPermission,
                        accentColor = accentColor,
                        onRequest = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }

                // Direct shortcut to app visual metadata
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToAbout() },
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Tentang Aplikasi",
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Buka Tentang AturVol",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Info developer, lisensi, dan arsitektur",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowRight,
                            contentDescription = "Selanjutnya",
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 12.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSegmentedRow(
    title: String,
    subtitle: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    accentColor: Color,
    onSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            options.forEach { (key, label) ->
                val isSelected = selectedValue == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accentColor else Color.Transparent)
                        .clickable { onSelected(key) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF0F172A) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSegmentedRow3(
    title: String,
    subtitle: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    accentColor: Color,
    onSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (key, label) ->
                val isSelected = selectedValue == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accentColor else Color.Transparent)
                        .clickable { onSelected(key) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF0F172A) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
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
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun ThemeOptionSelector(
    name: String,
    colorSymbol: Color,
    bgSample: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgSample)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colorSymbol else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(colorSymbol, CircleShape)
            )
            Text(
                text = name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    accentColor: Color,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .background(Color(0x1F10B981), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Izinkan Aktif",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Aktif",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Berikan",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
