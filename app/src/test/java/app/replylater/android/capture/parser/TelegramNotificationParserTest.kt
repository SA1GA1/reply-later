package app.replylater.android.capture.parser

import app.replylater.android.capture.model.Messenger
import app.replylater.android.capture.model.RawMessage
import app.replylater.android.capture.model.RawNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramNotificationParserTest {
    private val parser = TelegramNotificationParser()

    @Test
    fun acceptsExplicitDirectMessagingStyleNotification() {
        val result = parser.parse(
            rawTelegram(
                title = "Илья",
                text = "Старый текст",
                isGroupConversation = false,
                messages = listOf(
                    RawMessage("Илья", "Первое сообщение", 1_000L),
                    RawMessage("Илья", "Скинь, пожалуйста, ссылку", 2_000L),
                ),
            ),
        )

        val message = (result as ParseResult.Accepted).message
        assertEquals(Messenger.TELEGRAM, message.messenger)
        assertEquals("Илья", message.contactDisplayName)
        assertEquals("Скинь, пожалуйста, ссылку", message.messageText)
        assertEquals(2_000L, message.receivedAtEpochMillis)
        assertEquals("telegram-key", message.notificationKey)
        assertEquals(64, message.conversationKey.length)
        assertFalse(message.conversationKey.contains("Илья"))
    }

    @Test
    fun acceptsUnambiguousSingleSenderWhenGroupFlagIsMissing() {
        val result = parser.parse(
            rawTelegram(
                title = "Илья",
                isGroupConversation = null,
                messages = listOf(RawMessage("Илья", "Привет", 2_000L)),
            ),
        )

        assertTrue(result is ParseResult.Accepted)
    }

    @Test
    fun rejectsExplicitGroupConversation() {
        val result = parser.parse(
            rawTelegram(
                title = "Команда",
                conversationTitle = "Команда",
                isGroupConversation = true,
            ),
        )

        assertEquals(
            RejectReason.GROUP_CHAT,
            (result as ParseResult.Rejected).reason,
        )
    }

    @Test
    fun rejectsChannelMarker() {
        val result = parser.parse(
            rawTelegram(
                title = "Новости",
                subText = "Канал",
                isGroupConversation = null,
            ),
        )

        assertEquals(
            RejectReason.CHANNEL_OR_BOT,
            (result as ParseResult.Rejected).reason,
        )
    }

    @Test
    fun rejectsConflictingConversationSignals() {
        val result = parser.parse(
            rawTelegram(
                title = "Илья",
                conversationTitle = "Команда",
                isGroupConversation = null,
                messages = listOf(RawMessage("Илья", "Привет", 2_000L)),
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
            rawTelegram(
                title = "Команда",
                isGroupConversation = null,
                messages = listOf(
                    RawMessage("Илья", "Привет", 1_000L),
                    RawMessage("Анна", "Добрый день", 2_000L),
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
            rawTelegram(
                title = "  ",
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
            rawTelegram(
                title = "Telegram",
                category = "progress",
                isGroupConversation = false,
            ),
        )

        assertEquals(
            RejectReason.NOT_A_MESSAGE,
            (result as ParseResult.Rejected).reason,
        )
    }
}

private fun rawTelegram(
    title: String? = null,
    text: String? = null,
    subText: String? = null,
    conversationTitle: String? = null,
    category: String? = "msg",
    isGroupConversation: Boolean? = null,
    messages: List<RawMessage> = emptyList(),
) = RawNotification(
    packageName = SupportedNotificationParser.TELEGRAM_PACKAGE,
    notificationKey = "telegram-key",
    postedAtEpochMillis = 3_000L,
    title = title,
    text = text,
    subText = subText,
    conversationTitle = conversationTitle,
    category = category,
    isGroupConversation = isGroupConversation,
    messages = messages,
)

