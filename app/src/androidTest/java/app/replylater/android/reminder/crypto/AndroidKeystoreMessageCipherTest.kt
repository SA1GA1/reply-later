package app.replylater.android.reminder.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreMessageCipherTest {
    private val cipher = AndroidKeystoreMessageCipher()

    @Test
    fun encryptDecryptRoundTripUsesRandomizedOutput() {
        val first = cipher.encrypt("MESSAGE_1")
        val second = cipher.encrypt("MESSAGE_1")

        assertEquals("MESSAGE_1", cipher.decrypt(first))
        assertEquals("MESSAGE_1", cipher.decrypt(second))
        assertFalse(first.iv.contentEquals(second.iv))
        assertFalse(first.ciphertext.contentEquals(second.ciphertext))
    }
}
