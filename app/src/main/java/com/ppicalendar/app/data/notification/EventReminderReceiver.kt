package com.ppicalendar.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Upcoming Event"
        val company = intent.getStringExtra("company") ?: "Placement Event"
        val venue = intent.getStringExtra("venue")
        val time = intent.getStringExtra("time") ?: ""

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showEventReminderNotification(title, company, venue, time)
    }
}
