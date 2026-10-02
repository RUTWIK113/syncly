package com.ppicalendar.app.presentation.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ppicalendar.app.ui.theme.DarkSurface
import com.ppicalendar.app.ui.theme.PureBlack
import com.ppicalendar.app.ui.theme.PureWhite
import com.ppicalendar.app.ui.theme.SynclyPrimaryAmber

enum class RatingPromptType(val maxStars: Int) {
    FIVE_STAR(5),
    TEN_STAR(10)
}

@Composable
fun AppRatingDialog(
    promptType: RatingPromptType,
    appOpenCount: Int,
    onDismiss: () -> Unit,
    onSubmitRating: (rating: Int, maxStars: Int, reviewText: String) -> Unit
) {
    val context = LocalContext.current
    var selectedStars by remember { mutableIntStateOf(if (promptType == RatingPromptType.FIVE_STAR) 5 else 10) }
    var reviewText by remember { mutableStateOf("") }

    val isFiveStar = promptType == RatingPromptType.FIVE_STAR

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SynclyPrimaryAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = SynclyPrimaryAmber,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isFiveStar) "Enjoying Syncly?" else "Syncly Milestone Rating",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isFiveStar) {
                        "You've used Syncly 5 times! How's your experience organizing placements so far?"
                    } else {
                        "You've reached $appOpenCount app opens! Please rate your experience out of 10:"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Star Picker
                if (isFiveStar) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 1..5) {
                            val isSelected = i <= selectedStars
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "$i Stars",
                                tint = if (isSelected) SynclyPrimaryAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(42.dp)
                                    .padding(horizontal = 3.dp)
                                    .clickable { selectedStars = i }
                            )
                        }
                    }
                } else {
                    // 10-Star Picker: Scrollable / Clean 2-row or chip selection
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            for (i in 1..10) {
                                val isSelected = i == selectedStars
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) SynclyPrimaryAmber else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) SynclyPrimaryAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedStars = i },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$i",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) PureBlack else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Selected: $selectedStars / 10 ⭐",
                            fontWeight = FontWeight.SemiBold,
                            color = SynclyPrimaryAmber,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Feedback input
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Comments / Suggestions (Optional)") },
                    placeholder = { Text("Let the developer know how to make Syncly even better...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SynclyPrimaryAmber,
                        focusedLabelColor = SynclyPrimaryAmber,
                        cursorColor = SynclyPrimaryAmber
                    )
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Maybe Later",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            dispatchRatingEmail(
                                context = context,
                                stars = selectedStars,
                                maxStars = promptType.maxStars,
                                review = reviewText,
                                appOpens = appOpenCount
                            )
                            onSubmitRating(selectedStars, promptType.maxStars, reviewText)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SynclyPrimaryAmber,
                            contentColor = PureBlack
                        )
                    ) {
                        Text(
                            text = "Submit Review",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun dispatchRatingEmail(
    context: Context,
    stars: Int,
    maxStars: Int,
    review: String,
    appOpens: Int
) {
    val developerEmail = "aadharamos113@gmail.com"
    val subject = "[Syncly Review - $maxStars Stars] Rating: $stars/$maxStars"
    val body = buildString {
        appendLine("⭐ Syncly App Rating")
        appendLine("═════════════════════════════════")
        appendLine("Score: $stars / $maxStars Stars")
        appendLine("Total App Opens: $appOpens")
        appendLine("Date: ${java.time.LocalDateTime.now()}")
        appendLine()
        appendLine("💬 User Feedback:")
        appendLine(if (review.isNotBlank()) review.trim() else "(No additional comments written)")
        appendLine()
        appendLine("📱 Device Diagnostics:")
        appendLine("• Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("• Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("• App Version: Syncly v1.0.0")
    }

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(developerEmail))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    try {
        context.startActivity(Intent.createChooser(intent, "Send Rating via Email"))
        Toast.makeText(context, "Thank you for rating Syncly! ⭐", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found to send review.", Toast.LENGTH_SHORT).show()
    }
}
