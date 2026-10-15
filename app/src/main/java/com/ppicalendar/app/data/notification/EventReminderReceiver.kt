package com.ppicalendar.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ppicalendar.app.presentation.AlarmRingingActivity

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra("eventId", 0L)
        val company = intent.getStringExtra("company") ?: "Placement Event"
        val title = intent.getStringExtra("title") ?: "Upcoming Event"
        val venue = intent.getStringExtra("venue")
        val date = intent.getStringExtra("date") ?: ""
        val time = intent.getStringExtra("time") ?: ""

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showEventReminderNotification(
            eventId = eventId,
            company = company,
            title = title,
            venue = venue,
            time = time,
            date = date
        )

        try {
            val alarmIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("eventId", eventId)
                putExtra("company", company)
                putExtra("title", title)
                putExtra("venue", venue)
                putExtra("date", date)
                putExtra("time", time)
            }
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
