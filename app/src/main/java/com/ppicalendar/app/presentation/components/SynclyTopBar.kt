package com.ppicalendar.app.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ppicalendar.app.ui.theme.PureBlack
import com.ppicalendar.app.ui.theme.SynclyDarkBg
import com.ppicalendar.app.ui.theme.SynclyPrimaryAmber
import com.ppicalendar.app.ui.theme.SynclyTextMuted

@Composable
fun SynclyHeader(
    title: String,
    subtitle: String? = null,
    logo: Painter? = null,
    onBackClick: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == SynclyDarkBg

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isDark) Color.Transparent else SynclyPrimaryAmber,
        shape = if (isDark) RoundedCornerShape(0.dp) else RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (isDark) 12.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isDark) SynclyPrimaryAmber else PureBlack,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (logo != null) {
                        Image(
                            painter = logo,
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) SynclyPrimaryAmber else PureBlack,
                        letterSpacing = (-0.3).sp
                    )
                }

                if (actions != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        actions()
                    }
                }
            }

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF23242B)
                )
            }
        }
    }
}

@Composable
fun SynclyFooter(modifier: Modifier = Modifier) {
    var tapCount by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var showAdminDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Crafted by Rutwik",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = SynclyTextMuted,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable {
                tapCount++
                if (tapCount >= 5) {
                    tapCount = 0
                    showAdminDialog = true
                }
            }
        )
    }

    if (showAdminDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAdminDialog = false },
            title = {
                Text("👑 Syncly Admin & Developer Portal", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "• Private Dashboard: Stored locally in /dashboard/index.html. Excluded in .gitignore to remain private from GitHub.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• WhatsApp APK Sharing: You can send 'app-debug.apk' directly to your WhatsApp groups. Users can install it with one tap.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Feedback Destination: Sent secretly to your configured email.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAdminDialog = false }) {
                    Text("Close", color = SynclyPrimaryAmber, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
