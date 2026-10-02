package com.ppicalendar.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ppicalendar.app.PPICalendarApplication
import com.ppicalendar.app.domain.model.EventStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionRx"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(NotificationHelper.EXTRA_EVENT_ID, -1L)
        if (eventId == -1L) return

        val app = context.applicationContext as? PPICalendarApplication ?: return
        val container = app.container

        // Cancel notification popup
        container.notificationHelper.cancelNotification(eventId.toInt())

        val action = intent.action
        Log.d(TAG, "Received action: $action for event ID: $eventId")

        CoroutineScope(Dispatchers.IO).launch {
            val event = container.placementEventRepository.getEventById(eventId) ?: return@launch

            when (action) {
                NotificationHelper.ACTION_CONFIRM_EVENT -> {
                    val result = container.createCalendarEventUseCase(event)
                    result.onSuccess {
                        val updatedEvent = container.placementEventRepository.getEventById(eventId)
                        if (updatedEvent != null) {
                            container.notificationHelper.showEventCreatedNotification(updatedEvent)
                        }
                    }.onFailure { e ->
                        Log.e(TAG, "Failed to create calendar event from notification action: ${e.message}", e)
                    }
                }
                NotificationHelper.ACTION_DISMISS_EVENT -> {
                    container.placementEventRepository.updateEventStatus(eventId, EventStatus.DISMISSED)
                }
            }
        }
    }
}
