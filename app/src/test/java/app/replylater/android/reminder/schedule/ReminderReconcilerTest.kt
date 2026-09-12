package app.replylater.android.reminder.schedule

import app.replylater.android.reminder.domain.Reminder
import app.replylater.android.reminder.domain.ReminderRepository
import app.replylater.android.reminder.domain.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderReconcilerTest {
    @Test
    fun schedulesFutureItemsAndReturnsOverdueIds() = runTest {
        val repository = StaticReminderRepository(
            listOf(
                reminder("OVERDUE", remindAt = 999L),
                reminder("FUTURE", remindAt = 2_000L),
            ),
        )
        val scheduler = RecordingReminderScheduler()
        val reconciler = ReminderReconciler(
            repository = repository,
            scheduler = scheduler,
            nowEpochMillis = { 1_000L },
        )

        val overdueIds = reconciler.reconcile()

        assertEquals(listOf("OVERDUE"), overdueIds)
        assertEquals(listOf("FUTURE" to 2_000L), scheduler.scheduled)
    }

    private fun reminder(id: String, remindAt: Long) = Reminder(
        id = id,
        sourcePackage = "com.whatsapp",
        conversationKey = "CONVERSATION_$id",
        contactDisplayName = "CONTACT_A",
        messageText = "MESSAGE_1",
        receivedAtEpochMillis = 100L,
        remindAtEpochMillis = remindAt,
        answeredAtEpochMillis = null,
        createdAtEpochMillis = 200L,
        updatedAtEpochMillis = 200L,
        sourceNotificationKey = "SOURCE_KEY",
    )
}

private class StaticReminderRepository(
    private val reminders: List<Reminder>,
) : ReminderRepository {
    override suspend fun upsert(reminder: Reminder) = Unit
    override suspend fun get(id: String): Reminder? = reminders.firstOrNull { it.id == id }
    override fun observeAll(): Flow<List<Reminder>> = flowOf(reminders)
    override suspend fun unfinished(): List<Reminder> = reminders
    override suspend fun markAnswered(id: String, answeredAtEpochMillis: Long) = Unit
    override suspend fun reschedule(id: String, remindAtEpochMillis: Long, updatedAtEpochMillis: Long) = Unit
    override suspend fun delete(id: String) = Unit
    override suspend fun deleteAll() = Unit
}

private class RecordingReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Pair<String, Long>>()

    override fun schedule(reminderId: String, remindAtEpochMillis: Long) {
        scheduled += reminderId to remindAtEpochMillis
    }

    override fun cancel(reminderId: String) = Unit
}
