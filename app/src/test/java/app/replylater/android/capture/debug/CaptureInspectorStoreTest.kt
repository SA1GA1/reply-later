package app.replylater.android.capture.debug

import app.replylater.android.capture.model.Messenger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CaptureInspectorStoreTest {
    @Test
    fun contentVisibilityStartsDisabled() {
        assertFalse(CaptureInspectorStore.showContent.value)
    }

    @Test
    fun recordKeepsOnlyFiftyNewestEntries() {
        repeat(55) { index ->
            CaptureInspectorStore.record(
                InspectorEntry(
                    timestampEpochMillis = index.toLong(),
                    messenger = Messenger.TELEGRAM,
                    accepted = true,
                    rejectionReason = null,
                    hadTitle = true,
                    hadText = true,
                    hadConversationTitle = false,
                    messageCount = 1,
                    contactDisplayName = "CONTACT_$index",
                    messagePreview = "MESSAGE_$index",
                ),
            )
        }

        val entries = CaptureInspectorStore.entries.value
        assertEquals(50, entries.size)
        assertEquals(54L, entries.first().timestampEpochMillis)
        assertEquals(5L, entries.last().timestampEpochMillis)
    }
}

