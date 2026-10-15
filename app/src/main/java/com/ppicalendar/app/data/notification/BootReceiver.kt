package com.ppicalendar.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ppicalendar.app.PPICalendarApplication
import com.ppicalendar.app.domain.model.EventStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? PPICalendarApplication
                    val repository = app?.container?.placementEventRepository
                    val notificationHelper = app?.container?.notificationHelper ?: NotificationHelper(context)

                    val events = repository?.getAllEventsImmediate() ?: emptyList()
                    events.forEach { event ->
                        if (event.status != EventStatus.DISMISSED) {
                            notificationHelper.scheduleEventReminder(event)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
