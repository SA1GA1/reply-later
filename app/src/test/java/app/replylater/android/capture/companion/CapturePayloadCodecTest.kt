package app.replylater.android.capture.companion

import app.replylater.android.capture.model.Messenger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CapturePayloadCodecTest {
    @Test
    fun roundTripsSupportedSyntheticPayload() {
        val payload = payload()

        assertEquals(payload, CapturePayloadCodec.decode(CapturePayloadCodec.encode(payload)))
    }

    @Test
    fun rejectsMessengerAndPackageMismatch() {
        val encoded = CapturePayloadCodec.encode(payload()).toMutableMap().apply {
            this[CapturePayloadCodec.SOURCE_PACKAGE] = "org.telegram.messenger"
        }

        assertNull(CapturePayloadCodec.decode(encoded))
    }

    @Test
    fun rejectsMissingConversationIdentity() {
        val encoded = CapturePayloadCodec.encode(payload()).toMutableMap().apply {
            remove(CapturePayloadCodec.CONVERSATION_KEY)
        }

        assertNull(CapturePayloadCodec.decode(encoded))
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
