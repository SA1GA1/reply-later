package app.replylater.android.capture.framework

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.replylater.android.capture.model.Messenger
import app.replylater.android.capture.parser.ParseResult
import app.replylater.android.capture.parser.RejectReason
import app.replylater.android.capture.parser.SupportedNotificationParser
import app.replylater.android.capture.parser.TelegramNotificationParser
import app.replylater.android.capture.parser.WhatsAppNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationPipelineInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val normalizer = NotificationNormalizer()
    private val parser = SupportedNotificationParser(
        telegramParser = TelegramNotificationParser(),
        whatsappParser = WhatsAppNotificationParser(),
    )

    @Test
    fun canonicalWhatsAppDirectMessagePassesPlatformPipeline() {
        val notification = messageNotification(
            conversationTitle = null,
            isGroupConversation = false,
            messages = listOf("CONTACT_A" to "MESSAGE_1"),
        )

        val raw = normalizer.normalize(
            packageName = SupportedNotificationParser.WHATSAPP_PACKAGE,
            notificationKey = "whatsapp-direct",
            postedAtEpochMillis = 2_000L,
            notification = notification,
        )
        val result = parser.parse(requireNotNull(raw))

        assertTrue(result is ParseResult.Accepted)
        val message = (result as ParseResult.Accepted).message
        assertEquals(Messenger.WHATSAPP, message.messenger)
        assertEquals("CONTACT_A", message.contactDisplayName)
        assertEquals("MESSAGE_1", message.messageText)
    }

    @Test
    fun canonicalWhatsAppGroupIsRejectedByPlatformPipeline() {
        val notification = messageNotification(
            conversationTitle = "GROUP_A",
            isGroupConversation = true,
            messages = listOf(
                "CONTACT_A" to "MESSAGE_1",
                "CONTACT_B" to "MESSAGE_2",
            ),
        )

        val raw = normalizer.normalize(
            packageName = SupportedNotificationParser.WHATSAPP_PACKAGE,
            notificationKey = "whatsapp-group",
            postedAtEpochMillis = 3_000L,
            notification = notification,
        )
        val result = parser.parse(requireNotNull(raw))

        assertEquals(
            RejectReason.GROUP_CHAT,
            (result as ParseResult.Rejected).reason,
        )
    }

    private fun messageNotification(
        conversationTitle: String?,
        isGroupConversation: Boolean,
        messages: List<Pair<String, String>>,
    ): Notification {
        val style = NotificationCompat.MessagingStyle(person("DEVICE_OWNER"))
            .setGroupConversation(isGroupConversation)
        conversationTitle?.let(style::setConversationTitle)
        messages.forEachIndexed { index, (sender, text) ->
            style.addMessage(text, 1_000L + index, person(sender))
        }

        return NotificationCompat.Builder(context, "instrumented-test")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(style)
            .build()
    }

    private fun person(name: String): Person = Person.Builder()
        .setName(name)
        .build()
}

