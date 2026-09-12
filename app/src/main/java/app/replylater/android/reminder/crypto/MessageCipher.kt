package app.replylater.android.reminder.crypto

data class EncryptedMessage(
    val ciphertext: ByteArray,
    val iv: ByteArray,
)

interface MessageCipher {
    fun encrypt(plaintext: String): EncryptedMessage

    fun decrypt(encrypted: EncryptedMessage): String
}
