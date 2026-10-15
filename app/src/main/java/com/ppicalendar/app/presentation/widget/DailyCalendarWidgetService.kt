package com.ppicalendar.app.presentation.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.ppicalendar.app.R
import com.ppicalendar.app.data.local.AppDatabase
import com.ppicalendar.app.data.local.entity.PlacementEventEntity
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DailyCalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return DailyCalendarRemoteViewsFactory(applicationContext)
    }
}

class DailyCalendarRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var todayEvents: List<PlacementEventEntity> = emptyList()

    override fun onCreate() {
        // Initial setup
    }

    override fun onDataSetChanged() {
        try {
            val database = AppDatabase.getInstance(context)
            val allEvents: List<PlacementEventEntity> = runBlocking {
                database.placementEventDao().getAllEventsImmediate()
            }

            val today = LocalDate.now()
            val isoDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE) // yyyy-MM-dd
            val dmyDate = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
            val dmyDash = today.format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault()))

            todayEvents = allEvents.filter { event ->
                event.status != "DISMISSED" && (
                    event.date == isoDate || event.date == dmyDate || event.date == dmyDash ||
                    event.deadlineDate == isoDate || event.deadlineDate == dmyDate || event.deadlineDate == dmyDash
                )
            }.sortedWith(compareBy({ it.startTime.ifBlank { "99:99" } }, { it.company }))
        } catch (e: Exception) {
            todayEvents = emptyList()
        }
    }

    override fun onDestroy() {
        todayEvents = emptyList()
    }

    override fun getCount(): Int = todayEvents.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= todayEvents.size) return null

        val event = todayEvents[position]
        val views = RemoteViews(context.packageName, R.layout.widget_event_item)

        // Time display
        val timeDisplay = when {
            !event.startTime.isNullOrBlank() -> event.startTime
            !event.deadlineTime.isNullOrBlank() -> event.deadlineTime
            else -> "All Day"
        }
        views.setTextViewText(R.id.widget_item_time, timeDisplay)

        // Company
        val companyDisplay = if (event.company.isNullOrBlank() || event.company == "Unknown") {
            "-------"
        } else {
            event.company
        }
        views.setTextViewText(R.id.widget_item_company, companyDisplay)

        // Event Type
        val typeDisplay = if (event.eventType.isNotBlank()) event.eventType.uppercase(Locale.getDefault()) else "EVENT"
        views.setTextViewText(R.id.widget_item_type, typeDisplay)

        // Venue
        val venueDisplay = when {
            !event.venue.isNullOrBlank() -> "📍 ${event.venue}"
            !event.meetingUrl.isNullOrBlank() -> "🔗 Online / Link"
            else -> "💻 Online"
        }
        views.setTextViewText(R.id.widget_item_venue, venueDisplay)

        // Fill-in Intent for list item click
        val fillInIntent = Intent().apply {
            putExtra(DailyCalendarWidgetProvider.EXTRA_EVENT_ID, event.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in todayEvents.indices) todayEvents[position].id else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
