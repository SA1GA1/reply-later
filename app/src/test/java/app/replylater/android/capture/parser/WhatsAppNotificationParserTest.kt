package app.replylater.android.capture.parser

import app.replylater.android.capture.model.Messenger
import app.replylater.android.capture.model.RawMessage
import app.replylater.android.capture.model.RawNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppNotificationParserTest {
    private val parser = WhatsAppNotificationParser()

    @Test
    fun acceptsExplicitDirectMessagingStyleNotification() {
        val result = parser.parse(
            rawWhatsApp(
                title = "Анна",
                text = "Старый текст",
                isGroupConversation = false,
                messages = listOf(
                    RawMessage("Анна", "Первое сообщение", 1_000L),
                    RawMessage("Анна", "Будешь сегодня?", 2_000L),
                ),
            ),
        )

        val message = (result as ParseResult.Accepted).message
        assertEquals(Messenger.WHATSAPP, message.messenger)
        assertEquals("Анна", message.contactDisplayName)
        assertEquals("Будешь сегодня?", message.messageText)
        assertEquals(2_000L, message.receivedAtEpochMillis)
    }

    @Test
    fun acceptsUnambiguousSingleSenderWhenGroupFlagIsMissing() {
        val result = parser.parse(
            rawWhatsApp(
                title = "Анна",
                isGroupConversation = null,
                messages = listOf(RawMessage("Анна", "Привет", 2_000L)),
            ),
        )

        assertTrue(result is ParseResult.Accepted)
    }

    @Test
    fun rejectsExplicitGroupConversation() {
        val result = parser.parse(
            rawWhatsApp(
                title = "Семья",
                conversationTitle = "Семья",
                isGroupConversation = true,
            ),
        )

        assertEquals(
            RejectReason.GROUP_CHAT,
            (result as ParseResult.Rejected).reason,
        )
    }

    @Test
    fun rejectsBundledMultiChatSummary() {
        val result = parser.parse(
            rawWhatsApp(
                title = "WhatsApp",
                text = "3 новых сообщения из 2 чатов",
                isGroupConversation = null,
                messages = emptyList(),
            ),
        )

        assertEquals(
            RejectReason.AMBIGUOUS_CHAT,
            (result as ParseResult.Rejected).reason,
        )
    }

    @Test
    fun rejectsConflictingConversationSignals() {
        val result = parser.parse(
            rawWhatsApp(
                title = "Анна",
                conversationTitle = "Семья",
                isGroupConversation = null,
                messages = listOf(RawMessage("Анна", "Привет", 2_000L)),
            ),
        )

        assertEquals(
            RejectReason.AMBIGUOUS_CHAT,
            (result as ParseResult.Rejected).reason,
        )
    }

    @Test
    fun rejectsMultipleSendersWithoutDirectChatFlag() {
        val result = parser.parse(
            rawWhatsApp(
                title = "Семья",
                isGroupConversation = null,
                messages = listOf(
                    RawMessage("Анна", "Привет", 1_000L),
                    RawMessage("Илья", "Добрый день", 2_000L),
                ),
            ),
        )

        assertEquals(
            RejectReason.AMBIGUOUS_CHAT,
            (result as ParseResult.Rejected).reason,
        )
    }

    @Test
    fun rejectsMissingContact() {
        val result = parser.parse(
            rawWhatsApp(
                title = null,
                isGroupConversation = false,
            ),
        )

        assertEquals(
            RejectReason.MISSING_CONTACT,
            (result as ParseResult.Rejected).reason,
        )
    }

    @Test
    fun rejectsNonMessageCategory() {
        val result = parser.parse(
            rawWhatsApp(
                title = "WhatsApp",
                category = "service",
                isGroupConversation = false,
            ),
        )

        assertEquals(
            RejectReason.NOT_A_MESSAGE,
            (result as ParseResult.Rejected).reason,
        )
    }
}

private fun rawWhatsApp(
    title: String? = null,
    text: String? = null,
    subText: String? = null,
    conversationTitle: String? = null,
    category: String? = "msg",
    isGroupConversation: Boolean? = null,
    messages: List<RawMessage> = emptyList(),
) = RawNotification(
    packageName = SupportedNotificationParser.WHATSAPP_PACKAGE,
    notificationKey = "whatsapp-key",
    postedAtEpochMillis = 3_000L,
    title = title,
    text = text,
    subText = subText,
    conversationTitle = conversationTitle,
    category = category,
    isGroupConversation = isGroupConversation,
    messages = messages,
)

