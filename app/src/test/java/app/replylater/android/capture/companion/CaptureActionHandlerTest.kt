package app.replylater.android.capture.companion

import app.replylater.android.capture.model.Messenger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureActionHandlerTest {
    private val creator = RecordingCaptureReminderCreator()
    private val handler = CaptureActionHandler(
        reminderCreator = creator,
        nowEpochMillis = { 1_000L },
    )

    @Test
    fun expandDoesNotSave() = runTest {
        assertEquals(CaptureActionResult.Expanded, handler.handle(CaptureAction.EXPAND, payload()))
        assertTrue(creator.created.isEmpty())
    }

    @Test
    fun thirtyMinutesCreatesOneReminder() = runTest {
        val result = handler.handle(CaptureAction.SAVE_30_MINUTES, payload())

        assertEquals(CaptureActionResult.Saved("REMINDER_ID"), result)
        assertEquals(listOf(1_801_000L), creator.created.map { it.second })
    }

    @Test
    fun sixtyMinutesCreatesOneReminder() = runTest {
        handler.handle(CaptureAction.SAVE_60_MINUTES, payload())

        assertEquals(listOf(3_601_000L), creator.created.map { it.second })
    }

    @Test
    fun moreReturnsPayloadWithoutSaving() = runTest {
        assertEquals(
            CaptureActionResult.OpenCustom(payload()),
            handler.handle(CaptureAction.MORE, payload()),
        )
        assertTrue(creator.created.isEmpty())
    }

    private fun payload() = CapturePayload(
        messenger = Messenger.WHATSAPP,
        sourcePackage = "com.whatsapp",
        notificationKey = "SOURCE_KEY",
        conversationKey = "a".repeat(64),
        contactDisplayName = "CONTACT_A",
        messageText = "MESSAGE_1",
        receivedAtEpochMillis = 500L,
    )
}

private class RecordingCaptureReminderCreator : CaptureReminderCreator {
    val created = mutableListOf<Pair<CapturePayload, Long>>()

    override suspend fun create(payload: CapturePayload, remindAtEpochMillis: Long): String {
        created += payload to remindAtEpochMillis
        return "REMINDER_ID"
    }
}
