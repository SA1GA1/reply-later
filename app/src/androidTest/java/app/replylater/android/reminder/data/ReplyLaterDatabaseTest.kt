package app.replylater.android.reminder.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReplyLaterDatabaseTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        ReplyLaterDatabase::class.java,
    ).allowMainThreadQueries().build()
    private val dao = database.reminderDao()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun daoSupportsReminderLifecycleAndOrdering() = runBlocking {
        dao.upsert(entity(id = "LATER", remindAt = 3_000L))
        dao.upsert(entity(id = "SOONER", remindAt = 2_000L))

        assertEquals(listOf("SOONER", "LATER"), dao.unfinished().map(ReminderEntity::id))
        assertEquals(2, dao.observeAll().first().size)

        dao.reschedule("LATER", remindAtEpochMillis = 1_500L, updatedAtEpochMillis = 400L)
        assertEquals(listOf("LATER", "SOONER"), dao.unfinished().map(ReminderEntity::id))

        dao.markAnswered("LATER", answeredAtEpochMillis = 500L, updatedAtEpochMillis = 500L)
        assertEquals(listOf("SOONER"), dao.unfinished().map(ReminderEntity::id))

        dao.delete("SOONER")
        assertNull(dao.get("SOONER"))
        dao.deleteAll()
        assertEquals(emptyList<ReminderEntity>(), dao.observeAll().first())
    }

    private fun entity(id: String, remindAt: Long) = ReminderEntity(
        id = id,
        sourcePackage = "com.whatsapp",
        conversationKey = "CONVERSATION_HASH_$id",
        contactDisplayName = "CONTACT_A",
        messageCiphertext = "ciphertext".toByteArray(),
        messageIv = "twelve-byte!".toByteArray(),
        receivedAtEpochMillis = 100L,
        remindAtEpochMillis = remindAt,
        answeredAtEpochMillis = null,
        createdAtEpochMillis = 200L,
        updatedAtEpochMillis = 200L,
        sourceNotificationKey = "SOURCE_KEY",
    )
}
