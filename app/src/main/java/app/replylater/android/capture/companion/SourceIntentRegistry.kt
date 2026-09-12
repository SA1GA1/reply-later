package app.replylater.android.capture.companion

import android.app.PendingIntent
import java.util.concurrent.ConcurrentHashMap

class SourceIntentRegistry {
    data class Entry(
        val payload: CapturePayload,
        val contentIntent: PendingIntent?,
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    fun put(sourceNotificationKey: String, payload: CapturePayload, contentIntent: PendingIntent?) {
        entries[sourceNotificationKey] = Entry(payload, contentIntent)
    }

    fun get(sourceNotificationKey: String?): Entry? = sourceNotificationKey?.let(entries::get)

    fun remove(sourceNotificationKey: String): Entry? = entries.remove(sourceNotificationKey)
}
