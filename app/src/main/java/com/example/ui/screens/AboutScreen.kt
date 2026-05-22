package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VolumeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: VolumeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeThemeName by viewModel.theme.collectAsState()

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
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Tentang Aplikasi",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Spesifikasi developer & teknologi",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Scrollable Info Hub
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Application Branding Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App logo glyph block
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(accentColor.copy(alpha = 0.4f), Color.Transparent)
                                    )
                                )
                                .border(1.5.dp, accentColor, CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = "AturVol Logo",
                                tint = accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "AturVol",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Text(
                            text = "Android Native Volume Utility",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Text(
                            text = "Versi 1.0.0 (Build 2026.05)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Aplikasi utilitas premium yang didesain untuk menyederhanakan kontrol audio sistem Android secara instan lewat tepian layar. Mengutamakan performa tanpa lag, stabil di background, dan hemat memori.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Developer & licensing credit cards
                AboutItemRow(
                    title = "Developer Utama",
                    description = "Azhar (azharmmto@gmail.com)\nNative Mobile Developer",
                    icon = Icons.Rounded.Person,
                    accentColor = accentColor
                )

                AboutItemRow(
                    title = "Arsitektur & Pola Sintaks",
                    description = "Model-View-ViewModel (MVVM) dengan implementasi Jetpack Compose M3 & SharedPreferences reactive flow.",
                    icon = Icons.Rounded.Layers,
                    accentColor = accentColor
                )

                AboutItemRow(
                    title = "Teknologi Background",
                    description = "Foreground Service terisolasi dengan Custom State Registry & WindowManager TYPE_APPLICATION_OVERLAY.",
                    icon = Icons.Rounded.Memory,
                    accentColor = accentColor
                )

                AboutItemRow(
                    title = "Lisensi Kode",
                    description = "Lisensi Apache Versi 2.0. Hak cipta dilindungi undang-undang. Dapat digunakan pribadi secara bebas.",
                    icon = Icons.Rounded.Gavel,
                    accentColor = accentColor
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AboutItemRow(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
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

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
