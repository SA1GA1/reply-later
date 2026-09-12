package app.replylater.android.reminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val sourcePackage: String,
    val conversationKey: String,
    val contactDisplayName: String,
    val messageCiphertext: ByteArray?,
    val messageIv: ByteArray?,
    val receivedAtEpochMillis: Long,
    val remindAtEpochMillis: Long,
    val answeredAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val sourceNotificationKey: String?,
)
