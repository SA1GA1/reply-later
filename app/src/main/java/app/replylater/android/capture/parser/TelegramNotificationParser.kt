package app.replylater.android.capture.parser

import app.replylater.android.capture.model.DirectMessage
import app.replylater.android.capture.model.Messenger
import app.replylater.android.capture.model.RawNotification
import java.security.MessageDigest
import java.util.Locale

class TelegramNotificationParser : NotificationParser {
    override fun parse(notification: RawNotification): ParseResult {
        if (notification.packageName != SupportedNotificationParser.TELEGRAM_PACKAGE) {
            return ParseResult.Rejected(RejectReason.UNSUPPORTED_PACKAGE)
        }
        if (notification.category != MESSAGE_CATEGORY) {
            return ParseResult.Rejected(RejectReason.NOT_A_MESSAGE)
        }
        if (notification.isGroupConversation == true) {
            return ParseResult.Rejected(RejectReason.GROUP_CHAT)
        }
        if (notification.subText.containsChannelOrBotMarker()) {
            return ParseResult.Rejected(RejectReason.CHANNEL_OR_BOT)
        }

        val contact = notification.title.normalizedText()
            ?: return ParseResult.Rejected(RejectReason.MISSING_CONTACT)
        val conversationTitle = notification.conversationTitle.normalizedText()
        if (conversationTitle != null && !conversationTitle.equals(contact, ignoreCase = true)) {
            return ParseResult.Rejected(RejectReason.AMBIGUOUS_CHAT)
        }

        val newestMessage = notification.messages.maxByOrNull { it.timestampEpochMillis }
        if (notification.isGroupConversation == null) {
            val senders = notification.messages
                .mapNotNull { it.sender.normalizedText()?.lowercase(Locale.ROOT) }
                .distinct()
            val directSender = senders.singleOrNull()
            if (directSender == null || directSender != contact.lowercase(Locale.ROOT)) {
                return ParseResult.Rejected(RejectReason.AMBIGUOUS_CHAT)
            }
        }

        return ParseResult.Accepted(
            DirectMessage(
                messenger = Messenger.TELEGRAM,
                notificationKey = notification.notificationKey,
                conversationKey = hashedConversationKey(notification.packageName, contact),
                contactDisplayName = contact,
                messageText = newestMessage?.text.normalizedText() ?: notification.text.normalizedText(),
                receivedAtEpochMillis = newestMessage
                    ?.timestampEpochMillis
                    ?.takeIf { it > 0L }
                    ?: notification.postedAtEpochMillis,
            ),
        )
    }

    private fun String?.containsChannelOrBotMarker(): Boolean {
        val words = normalizedText()
            ?.lowercase(Locale.ROOT)
            ?.split(Regex("[^\\p{L}\\p{N}]+"))
            ?.filter(String::isNotBlank)
            .orEmpty()
        return words.any { it in CHANNEL_OR_BOT_MARKERS }
    }

    private companion object {
        const val MESSAGE_CATEGORY = "msg"
        val CHANNEL_OR_BOT_MARKERS = setOf("канал", "channel", "бот", "bot")
    }
}

internal fun String?.normalizedText(): String? = this
    ?.trim()
    ?.replace(Regex("\\s+"), " ")
    ?.takeIf(String::isNotEmpty)

internal fun hashedConversationKey(packageName: String, contactDisplayName: String): String {
    val normalizedInput = "$packageName\u0000${contactDisplayName.normalizedText()?.lowercase(Locale.ROOT)}"
    return MessageDigest
        .getInstance("SHA-256")
        .digest(normalizedInput.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

