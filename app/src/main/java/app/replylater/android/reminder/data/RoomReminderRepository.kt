package app.replylater.android.reminder.data

import app.replylater.android.reminder.crypto.EncryptedMessage
import app.replylater.android.reminder.crypto.MessageCipher
import app.replylater.android.reminder.domain.Reminder
import app.replylater.android.reminder.domain.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomReminderRepository(
    private val dao: ReminderDao,
    private val messageCipher: MessageCipher,
) : ReminderRepository {
    override suspend fun upsert(reminder: Reminder) {
        dao.upsert(reminder.toEntity(messageCipher))
    }

    override suspend fun get(id: String): Reminder? = dao.get(id)?.toDomain(messageCipher)

    override fun observeAll(): Flow<List<Reminder>> = dao.observeAll().map { entities ->
        entities.map { it.toDomain(messageCipher) }
    }

    override suspend fun unfinished(): List<Reminder> = dao.unfinished().map {
        it.toDomain(messageCipher)
    }

    override suspend fun markAnswered(id: String, answeredAtEpochMillis: Long) {
        dao.markAnswered(
            id = id,
            answeredAtEpochMillis = answeredAtEpochMillis,
            updatedAtEpochMillis = answeredAtEpochMillis,
        )
    }

    override suspend fun reschedule(
        id: String,
        remindAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ) {
        dao.reschedule(id, remindAtEpochMillis, updatedAtEpochMillis)
    }

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun deleteAll() = dao.deleteAll()
}

private fun Reminder.toEntity(messageCipher: MessageCipher): ReminderEntity {
    val encrypted = messageText?.let(messageCipher::encrypt)
    return ReminderEntity(
        id = id,
        sourcePackage = sourcePackage,
        conversationKey = conversationKey,
        contactDisplayName = contactDisplayName,
        messageCiphertext = encrypted?.ciphertext,
        messageIv = encrypted?.iv,
        receivedAtEpochMillis = receivedAtEpochMillis,
        remindAtEpochMillis = remindAtEpochMillis,
        answeredAtEpochMillis = answeredAtEpochMillis,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        sourceNotificationKey = sourceNotificationKey,
    )
}

private fun ReminderEntity.toDomain(messageCipher: MessageCipher): Reminder {
    check((messageCiphertext == null) == (messageIv == null)) {
        "Encrypted message fields are inconsistent"
    }
    val message = if (messageCiphertext != null && messageIv != null) {
        messageCipher.decrypt(EncryptedMessage(messageCiphertext, messageIv))
    } else {
        null
    }
    return Reminder(
        id = id,
        sourcePackage = sourcePackage,
        conversationKey = conversationKey,
        contactDisplayName = contactDisplayName,
        messageText = message,
        receivedAtEpochMillis = receivedAtEpochMillis,
        remindAtEpochMillis = remindAtEpochMillis,
        answeredAtEpochMillis = answeredAtEpochMillis,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        sourceNotificationKey = sourceNotificationKey,
    )
}
