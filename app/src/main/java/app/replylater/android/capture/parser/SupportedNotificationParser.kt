package app.replylater.android.capture.parser

import app.replylater.android.capture.model.RawNotification

class SupportedNotificationParser(
    private val telegramParser: NotificationParser,
    private val whatsappParser: NotificationParser,
) : NotificationParser {
    override fun parse(notification: RawNotification): ParseResult = when (notification.packageName) {
        TELEGRAM_PACKAGE -> telegramParser.parse(notification)
        WHATSAPP_PACKAGE -> whatsappParser.parse(notification)
        else -> ParseResult.Rejected(RejectReason.UNSUPPORTED_PACKAGE)
    }

    companion object {
        const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        const val WHATSAPP_PACKAGE = "com.whatsapp"
    }
}

