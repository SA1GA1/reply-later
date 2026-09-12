package app.replylater.android.capture.parser

import app.replylater.android.capture.model.DirectMessage
import app.replylater.android.capture.model.Messenger
import app.replylater.android.capture.model.RawNotification
import java.util.Locale

class WhatsAppNotificationParser : NotificationParser {
    override fun parse(notification: RawNotification): ParseResult {
        if (notification.packageName != SupportedNotificationParser.WHATSAPP_PACKAGE) {
            return ParseResult.Rejected(RejectReason.UNSUPPORTED_PACKAGE)
        }
        if (notification.category != MESSAGE_CATEGORY) {
            return ParseResult.Rejected(RejectReason.NOT_A_MESSAGE)
        }
        if (notification.isGroupConversation == true) {
            return ParseResult.Rejected(RejectReason.GROUP_CHAT)
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
                messenger = Messenger.WHATSAPP,
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

    private companion object {
        const val MESSAGE_CATEGORY = "msg"
    }
}

