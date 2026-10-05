package com.ppicalendar.app.ui

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.ppicalendar.app.PPICalendarApplication
import com.ppicalendar.app.domain.usecase.NotificationProcessOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate

class WhatsAppAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "WhatsAppAccService"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Request to receive view hierarchy events
        serviceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        serviceInfo.packageNames = arrayOf(WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE)
        serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        // Only process events from WhatsApp packages
        val pkg = event.packageName?.toString() ?: return
        if (pkg != WHATSAPP_PACKAGE && pkg != WHATSAPP_BUSINESS_PACKAGE) return

        val rootNode = rootInActiveWindow ?: return
        try {
            // Try to find the message text view – most WhatsApp builds use the ID "message_text"
            val messageNode = findNodeByViewId(rootNode, "com.whatsapp:id/message_text")
                ?: findNodeByViewId(rootNode, "com.whatsapp.w4b:id/message_text")
            val senderNode = findNodeByViewId(rootNode, "com.whatsapp:id/contact_name")
                ?: findNodeByViewId(rootNode, "com.whatsapp.w4b:id/contact_name")

            val message = messageNode?.text?.toString()?.trim() ?: return
            if (message.isBlank()) return
            val sender = senderNode?.text?.toString()?.trim() ?: ""
            val combined = if (sender.isNotBlank()) "$sender\n$message" else message

            // Use the same use‑case as the notification listener
            serviceScope.launch {
                val app = applicationContext as? PPICalendarApplication ?: return@launch
                val container = app.container
                val uniqueKey = "AccSrv-${System.currentTimeMillis()}"
                try {
                    val outcome = container.processNotificationUseCase(
                        notificationKey = uniqueKey,
                        sender = sender,
                        text = combined,
                        referenceDate = LocalDate.now()
                    )
                    Log.i(TAG, "Processed accessibility event: $outcome")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing accessibility event", e)
                }
            }
        } finally {
            // Recycle to avoid memory leaks
            rootNode.recycle()
        }
    }

    private fun findNodeByViewId(root: android.view.accessibility.AccessibilityNodeInfo, viewId: String): android.view.accessibility.AccessibilityNodeInfo? {
        val queue = java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            try {
                if (viewId == node.viewIdResourceName) return node
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }
            } finally {
                // do not recycle here – the caller recycles the root after use
            }
        }
        return null
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Accessibility service destroyed")
    }
}
