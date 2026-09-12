package app.replylater.android.capture.model

data class RawNotification(
    val packageName: String,
    val notificationKey: String,
    val postedAtEpochMillis: Long,
    val title: String?,
    val text: String?,
    val subText: String?,
    val conversationTitle: String?,
    val category: String?,
    val isGroupConversation: Boolean?,
    val messages: List<RawMessage>,
)

data class RawMessage(
    val sender: String?,
    val text: String?,
    val timestampEpochMillis: Long,
)

