package app.replylater.android.reminder.data

import app.replylater.android.reminder.crypto.EncryptedMessage
import app.replylater.android.reminder.crypto.MessageCipher
import app.replylater.android.reminder.domain.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomReminderRepositoryTest {
    private val dao = InMemoryReminderDao()
    private val repository = RoomReminderRepository(dao, PrefixMessageCipher())

    @Test
    fun upsertEncryptsMessageBeforeItReachesDao() = runTest {
        repository.upsert(reminder(messageText = "MESSAGE_1"))

        val stored = dao.items.value.single()
        assertArrayEquals("encrypted:MESSAGE_1".toByteArray(), stored.messageCiphertext)
        assertArrayEquals("unique-iv".toByteArray(), stored.messageIv)
        assertEquals("CONTACT_A", stored.contactDisplayName)
    }

    @Test
    fun getDecryptsMessageAtRepositoryBoundary() = runTest {
        repository.upsert(reminder(messageText = "MESSAGE_1"))

        assertEquals("MESSAGE_1", repository.get("REMINDER_ID")?.messageText)
    }

    @Test
    fun nullableMessageStaysNullWithoutCipherColumns() = runTest {
        repository.upsert(reminder(messageText = null))

        val stored = dao.items.value.single()
        assertNull(stored.messageCiphertext)
        assertNull(stored.messageIv)
        assertNull(repository.get("REMINDER_ID")?.messageText)
    }

    private fun reminder(messageText: String?) = Reminder(
        id = "REMINDER_ID",
        sourcePackage = "com.whatsapp",
        conversationKey = "CONVERSATION_HASH",
        contactDisplayName = "CONTACT_A",
        messageText = messageText,
        receivedAtEpochMillis = 100L,
        remindAtEpochMillis = 2_000L,
        answeredAtEpochMillis = null,
        createdAtEpochMillis = 200L,
        updatedAtEpochMillis = 200L,
        sourceNotificationKey = "SOURCE_KEY",
    )
}

private class PrefixMessageCipher : MessageCipher {
    override fun encrypt(plaintext: String): EncryptedMessage = EncryptedMessage(
        ciphertext = "encrypted:$plaintext".toByteArray(),
        iv = "unique-iv".toByteArray(),
    )

    override fun decrypt(encrypted: EncryptedMessage): String = encrypted.ciphertext
        .decodeToString()
        .removePrefix("encrypted:")
}

private class InMemoryReminderDao : ReminderDao {
    val items = MutableStateFlow<List<ReminderEntity>>(emptyList())

    override suspend fun upsert(entity: ReminderEntity) {
        items.value = items.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun get(id: String): ReminderEntity? = items.value.firstOrNull { it.id == id }

    override fun observeAll(): Flow<List<ReminderEntity>> = items

    override suspend fun unfinished(): List<ReminderEntity> = items.value
        .filter { it.answeredAtEpochMillis == null }
        .sortedBy { it.remindAtEpochMillis }

    override suspend fun markAnswered(
        id: String,
        answeredAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ) = Unit

    override suspend fun reschedule(
        id: String,
        remindAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ) = Unit

    override suspend fun delete(id: String) = Unit

    override suspend fun deleteAll() = Unit
}
