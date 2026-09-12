package app.replylater.android.reminder.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.replylater.android.reminder.crypto.AndroidKeystoreMessageCipher
import app.replylater.android.reminder.domain.Reminder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseAtRestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "encrypted-at-rest-test.db"
    private val database = Room.databaseBuilder(
        context,
        ReplyLaterDatabase::class.java,
        databaseName,
    ).build()
    private val repository = RoomReminderRepository(
        database.reminderDao(),
        AndroidKeystoreMessageCipher(),
    )

    @After
    fun deleteDatabase() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun plaintextMessageIsAbsentFromDatabaseFile() = runBlocking {
        repository.upsert(reminder())
        assertEquals("MESSAGE_1", repository.get("REMINDER_ID")?.messageText)
        database.close()

        val databaseBytes = context.getDatabasePath(databaseName).readBytes()
        val plaintextBytes = "MESSAGE_1".toByteArray()

        assertFalse(databaseBytes.containsSubsequence(plaintextBytes))
    }

    private fun reminder() = Reminder(
        id = "REMINDER_ID",
        sourcePackage = "com.whatsapp",
        conversationKey = "CONVERSATION_HASH",
        contactDisplayName = "CONTACT_A",
        messageText = "MESSAGE_1",
        receivedAtEpochMillis = 100L,
        remindAtEpochMillis = 2_000L,
        answeredAtEpochMillis = null,
        createdAtEpochMillis = 200L,
        updatedAtEpochMillis = 200L,
        sourceNotificationKey = "SOURCE_KEY",
    )
}

private fun ByteArray.containsSubsequence(candidate: ByteArray): Boolean {
    if (candidate.isEmpty() || candidate.size > size) return false
    return indices
        .take(size - candidate.size + 1)
        .any { start ->
            candidate.indices.all { offset -> this[start + offset] == candidate[offset] }
        }
}
