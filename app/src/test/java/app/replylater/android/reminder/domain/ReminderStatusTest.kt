package app.replylater.android.reminder.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderStatusTest {
    @Test
    fun futureUnansweredReminderIsPending() {
        assertEquals(
            ReminderStatus.PENDING,
            reminder(remindAtEpochMillis = 2_000L).statusAt(nowEpochMillis = 1_000L),
        )
    }

    @Test
    fun dueUnansweredReminderIsOverdue() {
        assertEquals(
            ReminderStatus.OVERDUE,
            reminder(remindAtEpochMillis = 1_000L).statusAt(nowEpochMillis = 1_000L),
        )
    }

    @Test
    fun answeredReminderStaysAnswered() {
        assertEquals(
            ReminderStatus.ANSWERED,
            reminder(
                remindAtEpochMillis = 999L,
                answeredAtEpochMillis = 500L,
            ).statusAt(nowEpochMillis = 1_000L),
        )
    }

    private fun reminder(
        remindAtEpochMillis: Long,
        answeredAtEpochMillis: Long? = null,
    ) = Reminder(
        id = "REMINDER_ID",
        sourcePackage = "com.whatsapp",
        conversationKey = "CONVERSATION_HASH",
        contactDisplayName = "CONTACT_A",
        messageText = "MESSAGE_1",
        receivedAtEpochMillis = 100L,
        remindAtEpochMillis = remindAtEpochMillis,
        answeredAtEpochMillis = answeredAtEpochMillis,
        createdAtEpochMillis = 200L,
        updatedAtEpochMillis = 200L,
        sourceNotificationKey = "SOURCE_KEY",
    )
}
