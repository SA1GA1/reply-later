package app.replylater.android.capture.companion

import app.replylater.android.capture.model.DirectMessage
import app.replylater.android.capture.model.Messenger

data class CapturePayload(
    val messenger: Messenger,
    val sourcePackage: String,
    val notificationKey: String,
    val conversationKey: String,
    val contactDisplayName: String,
    val messageText: String?,
    val receivedAtEpochMillis: Long,
) {
    fun toDirectMessage(): DirectMessage = DirectMessage(
        messenger = messenger,
        notificationKey = notificationKey,
        conversationKey = conversationKey,
        contactDisplayName = contactDisplayName,
        messageText = messageText,
        receivedAtEpochMillis = receivedAtEpochMillis,
    )
}

fun DirectMessage.toCapturePayload(sourcePackage: String): CapturePayload = CapturePayload(
    messenger = messenger,
    sourcePackage = sourcePackage,
    notificationKey = notificationKey,
    conversationKey = conversationKey,
    contactDisplayName = contactDisplayName,
    messageText = messageText,
    receivedAtEpochMillis = receivedAtEpochMillis,
)
