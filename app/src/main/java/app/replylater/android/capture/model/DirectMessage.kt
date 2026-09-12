package app.replylater.android.capture.model

enum class Messenger {
    TELEGRAM,
    WHATSAPP,
}

data class DirectMessage(
    val messenger: Messenger,
    val notificationKey: String,
    val conversationKey: String,
    val contactDisplayName: String,
    val messageText: String?,
    val receivedAtEpochMillis: Long,
)

