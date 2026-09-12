package app.replylater.android.reminder.domain

data class Reminder(
    val id: String,
    val sourcePackage: String,
    val conversationKey: String,
    val contactDisplayName: String,
    val messageText: String?,
    val receivedAtEpochMillis: Long,
    val remindAtEpochMillis: Long,
    val answeredAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val sourceNotificationKey: String?,
)

enum class ReminderStatus {
    PENDING,
    OVERDUE,
    ANSWERED,
}

fun Reminder.statusAt(nowEpochMillis: Long): ReminderStatus = when {
    answeredAtEpochMillis != null -> ReminderStatus.ANSWERED
    remindAtEpochMillis <= nowEpochMillis -> ReminderStatus.OVERDUE
    else -> ReminderStatus.PENDING
}
