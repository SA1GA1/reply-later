package app.replylater.android.reminder.domain

import app.replylater.android.capture.model.DirectMessage
import app.replylater.android.capture.model.Messenger
import java.util.UUID

class CreateReminder(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    suspend operator fun invoke(
        message: DirectMessage,
        remindAtEpochMillis: Long,
    ): String {
        val now = nowEpochMillis()
        require(remindAtEpochMillis > now) { "Reminder target must be in the future" }

        val reminder = Reminder(
            id = newId(),
            sourcePackage = message.messenger.sourcePackage,
            conversationKey = message.conversationKey,
            contactDisplayName = message.contactDisplayName,
            messageText = message.messageText,
            receivedAtEpochMillis = message.receivedAtEpochMillis,
            remindAtEpochMillis = remindAtEpochMillis,
            answeredAtEpochMillis = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            sourceNotificationKey = message.notificationKey,
        )

        repository.upsert(reminder)
        scheduler.schedule(reminder.id, reminder.remindAtEpochMillis)
        return reminder.id
    }
}

private val Messenger.sourcePackage: String
    get() = when (this) {
        Messenger.TELEGRAM -> "org.telegram.messenger"
        Messenger.WHATSAPP -> "com.whatsapp"
    }
