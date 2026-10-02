package com.ppicalendar.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ppicalendar.app.R
import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.presentation.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ALERTS_ID = "placement_alerts_channel"
        const val CHANNEL_SYNC_ID = "calendar_sync_channel"

        const val ACTION_CONFIRM_EVENT = "com.ppicalendar.app.ACTION_CONFIRM_EVENT"
        const val ACTION_DISMISS_EVENT = "com.ppicalendar.app.ACTION_DISMISS_EVENT"
        const val EXTRA_EVENT_ID = "extra_placement_event_id"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                context.getString(R.string.channel_placement_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_placement_alerts_desc)
                enableVibration(true)
            }

            val syncChannel = NotificationChannel(
                CHANNEL_SYNC_ID,
                context.getString(R.string.channel_calendar_sync),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_calendar_sync_desc)
            }

            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(syncChannel)
        }
    }

    fun showEventConfirmationNotification(event: PlacementEvent) {
        val notificationManager = NotificationManagerCompat.from(context)

        // Open App Intent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_EVENT_ID, event.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            event.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Add to Calendar" Action Intent
        val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CONFIRM_EVENT
            putExtra(EXTRA_EVENT_ID, event.id)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            (event.id * 10 + 1).toInt(),
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Dismiss" Action Intent
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_EVENT
            putExtra(EXTRA_EVENT_ID, event.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (event.id * 10 + 2).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🎯 ${event.company} • ${event.eventType.displayName}"
        val details = buildString {
            if (event.date.isNotBlank()) append("📅 Date: ${event.date}   ")
            if (event.startTime.isNotBlank()) append("⏰ Time: ${event.startTime}")
            if (!event.venue.isNullOrBlank()) append("\n📍 Venue: ${event.venue}")
            if (!event.meetingUrl.isNullOrBlank()) {
                val linkLabel = if (event.meetingUrl.contains("chat.whatsapp.com")) "WhatsApp Group" else "Link"
                append("\n💬 $linkLabel: ${event.meetingUrl}")
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Found on WhatsApp: ${event.date} at ${event.startTime}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "📅 Add to Calendar", confirmPendingIntent)

        // Direct "Join WhatsApp Group" or "Open Link" Action button
        if (!event.meetingUrl.isNullOrBlank()) {
            val linkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(event.meetingUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val linkPendingIntent = PendingIntent.getActivity(
                context,
                (event.id * 10 + 4).toInt(),
                linkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val actionLabel = if (event.meetingUrl.contains("chat.whatsapp.com")) {
                "👥 Join WA Group"
            } else {
                "🔗 Open Link"
            }
            builder.addAction(0, actionLabel, linkPendingIntent)
        }

        builder.addAction(0, "Dismiss", dismissPendingIntent)

        try {
            notificationManager.notify(event.id.toInt(), builder.build())
        } catch (e: SecurityException) {
            // Permission handling
        }
    }

    fun showEventCreatedNotification(event: PlacementEvent) {
        val notificationManager = NotificationManagerCompat.from(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (event.id * 10 + 3).toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val companySlug = event.company.lowercase().replace("[^a-zA-Z0-9]+".toRegex(), "_").trim('_')
        val typeSlug = when (event.eventType) {
            com.ppicalendar.app.domain.model.EventType.PPI,
            com.ppicalendar.app.domain.model.EventType.PRE_PLACEMENT_TALK -> "ppi"
            com.ppicalendar.app.domain.model.EventType.ONLINE_ASSESSMENT -> "test"
            com.ppicalendar.app.domain.model.EventType.INTERVIEW -> "interview"
            else -> event.eventType.name.lowercase()
        }
        val title = "synced_${companySlug}_$typeSlug"
        val body = "✅ Added to Calendar: ${event.company} • ${event.eventType.displayName}\n📅 ${event.date} at ${event.startTime}"

        val builder = NotificationCompat.Builder(context, CHANNEL_SYNC_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        // Direct WhatsApp group button on success notification if link exists
        if (!event.meetingUrl.isNullOrBlank()) {
            val linkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(event.meetingUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val linkPendingIntent = PendingIntent.getActivity(
                context,
                (event.id * 10 + 5).toInt(),
                linkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val actionLabel = if (event.meetingUrl.contains("chat.whatsapp.com")) "👥 Join WA Group" else "🔗 Open Link"
            builder.addAction(0, actionLabel, linkPendingIntent)
        }

        try {
            notificationManager.notify((event.id + 10000).toInt(), builder.build())
        } catch (e: SecurityException) {
            // Permission handling
        }
    }

    fun cancelNotification(notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
}
