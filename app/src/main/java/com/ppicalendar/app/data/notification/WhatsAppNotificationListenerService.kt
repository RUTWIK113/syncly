package com.ppicalendar.app.data.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import com.ppicalendar.app.PPICalendarApplication
import com.ppicalendar.app.domain.usecase.NotificationProcessOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.LocalDate

class WhatsAppNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "WhatsAppNotifListener"
        private val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false

            val myComponent = ComponentName(context, WhatsAppNotificationListenerService::class.java).flattenToString()
            return enabledListeners.split(":").any {
                val component = ComponentName.unflattenFromString(it)
                component != null && component.packageName == context.packageName
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        // 1. WhatsApp only
        val packageName = sbn.packageName ?: return
        if (!WHATSAPP_PACKAGES.contains(packageName)) {
            return
        }

        // 2. Ignore group summary notifications or ongoing calls
        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0 ||
            (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
            return
        }

        // 3. Extract text preview content safely from Android notification extras
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)

        val fullMessageContent = buildString {
            if (bigText.isNotBlank()) {
                append(bigText)
            } else if (!textLines.isNullOrEmpty()) {
                append(textLines.joinToString("\n"))
            } else if (text.isNotBlank()) {
                append(text)
            }
        }.trim()

        if (fullMessageContent.isBlank()) {
            return
        }

        // Combine title + text for placement extraction context (often title is the Group Name e.g. 'Placement IITM 2026' or Sender)
        val combinedText = if (title.isNotBlank()) "$title\n$fullMessageContent" else fullMessageContent

        // Generate unique notification key / hash to prevent re-processing identical messages
        val uniqueKey = generateNotificationHash(sbn.key, title, fullMessageContent)

        Log.d(TAG, "Processing WhatsApp notification from [$title] (Key: $uniqueKey)")

        val app = applicationContext as? PPICalendarApplication ?: return
        val container = app.container

        serviceScope.launch {
            try {
                val settings = container.settingsRepository.getSettings()

                val outcome = container.processNotificationUseCase(
                    notificationKey = uniqueKey,
                    sender = title,
                    text = combinedText,
                    referenceDate = LocalDate.now()
                )

                when (outcome) {
                    is NotificationProcessOutcome.MultipleProcessed -> {
                        Log.i(TAG, "Multiple events processed: ${outcome.created} created, ${outcome.requiredConfirmation} pending.")
                    }
                    is NotificationProcessOutcome.CreatedAutomatically -> {
                        Log.i(TAG, "Event automatically created for ${outcome.event.company}")
                        container.notificationHelper.showEventCreatedNotification(outcome.event)
                        checkAndAutoJoinWhatsAppGroup(outcome.event.meetingUrl, settings.autoJoinWhatsAppGroups)
                    }
                    is NotificationProcessOutcome.ConfirmationRequired -> {
                        Log.i(TAG, "Confirmation notification posted for ${outcome.event.company}")
                        container.notificationHelper.showEventConfirmationNotification(outcome.event)
                        checkAndAutoJoinWhatsAppGroup(outcome.event.meetingUrl, settings.autoJoinWhatsAppGroups)
                    }
                    is NotificationProcessOutcome.MissingEssentialInfo -> {
                        Log.i(TAG, "Missing info for ${outcome.event.company}, pending manual review")
                        container.notificationHelper.showEventConfirmationNotification(outcome.event)
                        checkAndAutoJoinWhatsAppGroup(outcome.event.meetingUrl, settings.autoJoinWhatsAppGroups)
                    }
                    is NotificationProcessOutcome.NoKeywordMatch -> {
                        Log.d(TAG, "Notification did not match placement keywords")
                    }
                    is NotificationProcessOutcome.NotPlacementEvent -> {
                        Log.d(TAG, "Notification classified as NOT a placement event")
                    }
                    is NotificationProcessOutcome.AlreadyProcessed -> {
                        Log.d(TAG, "Notification already processed previously")
                    }
                    is NotificationProcessOutcome.Disabled -> {
                        Log.d(TAG, "Notification processing disabled in settings")
                    }
                    is NotificationProcessOutcome.Error -> {
                        Log.e(TAG, "Error processing notification: ${outcome.message}", outcome.throwable)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unhandled exception in notification listener: ${e.message}", e)
            }
        }
    }

    private fun checkAndAutoJoinWhatsAppGroup(url: String?, isAutoJoinEnabled: Boolean) {
        if (isAutoJoinEnabled && !url.isNullOrBlank() && url.contains("chat.whatsapp.com", ignoreCase = true)) {
            try {
                val joinIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(joinIntent)
                Log.i(TAG, "Auto-joining WhatsApp group: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-join WhatsApp group: ${e.message}", e)
            }
        }
    }

    private fun generateNotificationHash(sbnKey: String, title: String, content: String): String {
        val raw = "${title.trim()}:${content.trim()}"
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(raw.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "${title.hashCode()}_${content.hashCode()}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
