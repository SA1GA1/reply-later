package app.replylater.android.capture.framework

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import app.replylater.android.capture.model.RawMessage
import app.replylater.android.capture.model.RawNotification
import app.replylater.android.capture.parser.SupportedNotificationParser

class NotificationNormalizer {
    fun normalize(statusBarNotification: StatusBarNotification): RawNotification? {
        if (!isSupportedPackage(statusBarNotification.packageName)) return null

        val notification = statusBarNotification.notification
        val extras = notification.extras
        val messagingStyle = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)
        val messages = messagingStyle
            ?.messages
            .orEmpty()
            .map { message ->
                RawMessage(
                    sender = normalizeText(message.person?.name),
                    text = normalizeText(message.text),
                    timestampEpochMillis = message.timestamp,
                )
            }
            .sortedBy(RawMessage::timestampEpochMillis)

        return RawNotification(
            packageName = statusBarNotification.packageName,
            notificationKey = statusBarNotification.key,
            postedAtEpochMillis = statusBarNotification.postTime,
            title = normalizeText(extras.getCharSequence(Notification.EXTRA_TITLE)),
            text = normalizeText(extras.getCharSequence(Notification.EXTRA_TEXT)),
            subText = normalizeText(extras.getCharSequence(Notification.EXTRA_SUB_TEXT)),
            conversationTitle = normalizeText(
                messagingStyle?.conversationTitle
                    ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
            ),
            category = notification.category,
            isGroupConversation = messagingStyle?.isGroupConversation,
            messages = messages,
        )
    }
}

internal fun normalizeText(value: CharSequence?): String? = value
    ?.toString()
    ?.trim()
    ?.replace(Regex("\\s+"), " ")
    ?.take(MAX_NOTIFICATION_TEXT_LENGTH)
    ?.takeIf(String::isNotEmpty)

internal fun isSupportedPackage(packageName: String): Boolean = packageName in setOf(
    SupportedNotificationParser.TELEGRAM_PACKAGE,
    SupportedNotificationParser.WHATSAPP_PACKAGE,
)

private const val MAX_NOTIFICATION_TEXT_LENGTH = 4_096
