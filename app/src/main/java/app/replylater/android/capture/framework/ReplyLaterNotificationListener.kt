package app.replylater.android.capture.framework

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.replylater.android.capture.debug.CaptureInspectorStore
import app.replylater.android.capture.debug.InspectorEntry
import app.replylater.android.ReplyLaterApplication
import app.replylater.android.capture.companion.toCapturePayload
import app.replylater.android.capture.model.Messenger
import app.replylater.android.capture.parser.ParseResult
import app.replylater.android.capture.parser.SupportedNotificationParser
import app.replylater.android.capture.parser.TelegramNotificationParser
import app.replylater.android.capture.parser.WhatsAppNotificationParser

class ReplyLaterNotificationListener : NotificationListenerService() {
    private val normalizer = NotificationNormalizer()
    private val parser = SupportedNotificationParser(
        telegramParser = TelegramNotificationParser(),
        whatsappParser = WhatsAppNotificationParser(),
    )

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        if (statusBarNotification.packageName == packageName) return

        val raw = normalizer.normalize(statusBarNotification) ?: return
        val result = parser.parse(raw)
        val accepted = result as? ParseResult.Accepted
        val rejected = result as? ParseResult.Rejected

        CaptureInspectorStore.record(
            InspectorEntry(
                timestampEpochMillis = System.currentTimeMillis(),
                messenger = accepted?.message?.messenger ?: raw.packageName.toMessenger(),
                accepted = accepted != null,
                rejectionReason = rejected?.reason,
                hadTitle = raw.title != null,
                hadText = raw.text != null || raw.messages.any { it.text != null },
                hadConversationTitle = raw.conversationTitle != null,
                messageCount = raw.messages.size,
                contactDisplayName = accepted?.message?.contactDisplayName,
                messagePreview = accepted?.message?.messageText,
            ),
        )

        accepted?.message?.let { message ->
            val graph = (applicationContext as ReplyLaterApplication).graph
            val payload = message.toCapturePayload(raw.packageName)
            graph.sourceIntentRegistry.put(
                sourceNotificationKey = message.notificationKey,
                payload = payload,
                contentIntent = statusBarNotification.notification.contentIntent,
            )
            graph.companionNotificationPublisher.publishInitial(payload)
        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        if (statusBarNotification.packageName !in SUPPORTED_PACKAGES) return
        val graph = (applicationContext as ReplyLaterApplication).graph
        graph.sourceIntentRegistry.remove(statusBarNotification.key)?.let { entry ->
            graph.companionNotificationPublisher.cancel(entry.payload)
        }
    }
}

private val SUPPORTED_PACKAGES = setOf(
    SupportedNotificationParser.TELEGRAM_PACKAGE,
    SupportedNotificationParser.WHATSAPP_PACKAGE,
)

private fun String.toMessenger(): Messenger? = when (this) {
    SupportedNotificationParser.TELEGRAM_PACKAGE -> Messenger.TELEGRAM
    SupportedNotificationParser.WHATSAPP_PACKAGE -> Messenger.WHATSAPP
    else -> null
}
