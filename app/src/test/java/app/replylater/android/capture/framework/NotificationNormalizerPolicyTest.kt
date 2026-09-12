package app.replylater.android.capture.framework

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationNormalizerPolicyTest {
    @Test
    fun normalizeTextCollapsesWhitespace() {
        assertEquals("Привет мир", normalizeText("  Привет\n  мир "))
    }

    @Test
    fun normalizeTextReturnsNullForBlankInput() {
        assertNull(normalizeText(" \n\t "))
    }

    @Test
    fun normalizeTextCapsUntrustedNotificationPayload() {
        assertEquals(4_096, normalizeText("я".repeat(5_000))?.length)
    }

    @Test
    fun supportedPackagePolicyIsStrict() {
        assertTrue(isSupportedPackage("org.telegram.messenger"))
        assertTrue(isSupportedPackage("com.whatsapp"))
        assertFalse(isSupportedPackage("org.thunderdog.challegram"))
        assertFalse(isSupportedPackage("com.whatsapp.w4b"))
    }
}

