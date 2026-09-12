package app.replylater.android.capture.parser

import app.replylater.android.capture.model.RawNotification
import org.junit.Assert.assertEquals
import org.junit.Test

class SupportedNotificationParserTest {
    private val telegramResult = ParseResult.Rejected(RejectReason.AMBIGUOUS_CHAT)
    private val whatsappResult = ParseResult.Rejected(RejectReason.GROUP_CHAT)
    private val router = SupportedNotificationParser(
        telegramParser = FixedResultParser(telegramResult),
        whatsappParser = FixedResultParser(whatsappResult),
    )

    @Test
    fun routesTelegramPackageToTelegramParser() {
        assertEquals(
            telegramResult,
            router.parse(rawNotification(packageName = "org.telegram.messenger")),
        )
    }

    @Test
    fun routesWhatsAppPackageToWhatsAppParser() {
        assertEquals(
            whatsappResult,
            router.parse(rawNotification(packageName = "com.whatsapp")),
        )
    }

    @Test
    fun rejectsUnsupportedPackage() {
        assertEquals(
            ParseResult.Rejected(RejectReason.UNSUPPORTED_PACKAGE),
            router.parse(rawNotification(packageName = "com.example.chat")),
        )
    }
}

private class FixedResultParser(
    private val result: ParseResult,
) : NotificationParser {
    override fun parse(notification: RawNotification): ParseResult = result
}

private fun rawNotification(packageName: String) = RawNotification(
    packageName = packageName,
    notificationKey = "notification-key",
    postedAtEpochMillis = 1_000L,
    title = null,
    text = null,
    subText = null,
    conversationTitle = null,
    category = "msg",
    isGroupConversation = null,
    messages = emptyList(),
)

