package app.replylater.android.reminder.domain

import app.replylater.android.capture.model.DirectMessage
import app.replylater.android.capture.model.Messenger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateReminderTest {
    private val events = mutableListOf<String>()
    private val repository = RecordingRepository(events)
    private val scheduler = RecordingScheduler(events)
    private val createReminder = CreateReminder(
        repository = repository,
        scheduler = scheduler,
        nowEpochMillis = { 1_000L },
        newId = { "REMINDER_ID" },
    )

    @Test
    fun explicitCreationPersistsThenSchedules() = runTest {
        val id = createReminder(message(), remindAtEpochMillis = 3_600_000L)

        assertEquals("REMINDER_ID", id)
        assertEquals(listOf("persist:REMINDER_ID", "schedule:REMINDER_ID"), events)
        assertEquals(
            Reminder(
                id = "REMINDER_ID",
                sourcePackage = "com.whatsapp",
                conversationKey = "CONVERSATION_HASH",
                contactDisplayName = "CONTACT_A",
                messageText = "MESSAGE_1",
                receivedAtEpochMillis = 500L,
                remindAtEpochMillis = 3_600_000L,
                answeredAtEpochMillis = null,
                createdAtEpochMillis = 1_000L,
                updatedAtEpochMillis = 1_000L,
                sourceNotificationKey = "SOURCE_KEY",
            ),
            repository.items.value.single(),
        )
        assertEquals(listOf("REMINDER_ID" to 3_600_000L), scheduler.scheduled)
    }

    @Test
    fun invalidTargetIsRejectedWithoutPersistence() = runTest {
        val failure = runCatching {
            createReminder(message(), remindAtEpochMillis = 1_000L)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(repository.items.value.isEmpty())
        assertTrue(scheduler.scheduled.isEmpty())
    }

    private fun message() = DirectMessage(
        messenger = Messenger.WHATSAPP,
        notificationKey = "SOURCE_KEY",
        conversationKey = "CONVERSATION_HASH",
        contactDisplayName = "CONTACT_A",
        messageText = "MESSAGE_1",
        receivedAtEpochMillis = 500L,
    )
}

private class RecordingRepository(
    private val events: MutableList<String>,
) : ReminderRepository {
    val items = MutableStateFlow<List<Reminder>>(emptyList())

    override suspend fun upsert(reminder: Reminder) {
        events += "persist:${reminder.id}"
        items.value = items.value.filterNot { it.id == reminder.id } + reminder
    }

    override suspend fun get(id: String): Reminder? = items.value.firstOrNull { it.id == id }

    override fun observeAll(): Flow<List<Reminder>> = items

    override suspend fun unfinished(): List<Reminder> = items.value.filter { it.answeredAtEpochMillis == null }

    override suspend fun markAnswered(id: String, answeredAtEpochMillis: Long) = Unit

    override suspend fun reschedule(
        id: String,
        remindAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ) = Unit

    override suspend fun delete(id: String) = Unit

    override suspend fun deleteAll() = Unit
}

private class RecordingScheduler(
    private val events: MutableList<String>,
) : ReminderScheduler {
    val scheduled = mutableListOf<Pair<String, Long>>()

    override fun schedule(reminderId: String, remindAtEpochMillis: Long) {
        events += "schedule:$reminderId"
        scheduled += reminderId to remindAtEpochMillis
    }

    override fun cancel(reminderId: String) = Unit
}
