package app.replylater.android.capture.companion

import app.replylater.android.capture.model.Messenger

object CapturePayloadCodec {
    const val MESSENGER = "messenger"
    const val SOURCE_PACKAGE = "source_package"
    const val NOTIFICATION_KEY = "notification_key"
    const val CONVERSATION_KEY = "conversation_key"
    const val CONTACT_NAME = "contact_name"
    const val MESSAGE_TEXT = "message_text"
    const val RECEIVED_AT = "received_at"

    fun encode(payload: CapturePayload): Map<String, String> = buildMap {
        put(MESSENGER, payload.messenger.name)
        put(SOURCE_PACKAGE, payload.sourcePackage)
        put(NOTIFICATION_KEY, payload.notificationKey)
        put(CONVERSATION_KEY, payload.conversationKey)
        put(CONTACT_NAME, payload.contactDisplayName)
        payload.messageText?.let { put(MESSAGE_TEXT, it) }
        put(RECEIVED_AT, payload.receivedAtEpochMillis.toString())
    }

    fun decode(values: Map<String, String>): CapturePayload? {
        val messenger = values[MESSENGER]
            ?.let { runCatching { Messenger.valueOf(it) }.getOrNull() }
            ?: return null
        val sourcePackage = values[SOURCE_PACKAGE]?.takeIf { it == messenger.sourcePackage }
            ?: return null
        val notificationKey = values[NOTIFICATION_KEY]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(MAX_TEXT_LENGTH)
            ?: return null
        val conversationKey = values[CONVERSATION_KEY]
            ?.takeIf { HASH_PATTERN.matches(it) }
            ?: return null
        val contact = values[CONTACT_NAME]
            ?.trim()
            ?.take(MAX_TEXT_LENGTH)
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val message = values[MESSAGE_TEXT]
            ?.trim()
            ?.take(MAX_TEXT_LENGTH)
            ?.takeIf { it.isNotEmpty() }
        val receivedAt = values[RECEIVED_AT]?.toLongOrNull()?.takeIf { it >= 0L }
            ?: return null
        return CapturePayload(
            messenger = messenger,
            sourcePackage = sourcePackage,
            notificationKey = notificationKey,
            conversationKey = conversationKey,
            contactDisplayName = contact,
            messageText = message,
            receivedAtEpochMillis = receivedAt,
        )
    }

    private val Messenger.sourcePackage: String
        get() = when (this) {
            Messenger.TELEGRAM -> "org.telegram.messenger"
            Messenger.WHATSAPP -> "com.whatsapp"
        }

    private val HASH_PATTERN = Regex("[0-9a-f]{64}")
    private const val MAX_TEXT_LENGTH = 4_096
}
