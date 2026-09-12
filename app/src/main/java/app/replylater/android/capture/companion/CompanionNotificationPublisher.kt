package app.replylater.android.capture.companion

import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.replylater.android.R
import app.replylater.android.notification.NotificationChannels
import app.replylater.android.notification.NotificationPostingPolicy

class CompanionNotificationPublisher(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(applicationContext)

    fun publishInitial(payload: CapturePayload) {
        publish(
            payload = payload,
            actions = listOf(CaptureAction.EXPAND to "Ответить позже"),
        )
    }

    fun publishExpanded(payload: CapturePayload) {
        publish(
            payload = payload,
            actions = listOf(
                CaptureAction.SAVE_30_MINUTES to "30 мин",
                CaptureAction.SAVE_60_MINUTES to "60 мин",
                CaptureAction.MORE to "Ещё",
            ),
        )
    }

    fun cancel(payload: CapturePayload) {
        notificationManager.cancel(notificationId(payload))
    }

    private fun publish(
        payload: CapturePayload,
        actions: List<Pair<CaptureAction, String>>,
    ) {
        val permissionGranted = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!NotificationPostingPolicy.canPost(Build.VERSION.SDK_INT, permissionGranted)) return

        NotificationChannels.ensureCaptureChannel(applicationContext)
        val builder = NotificationCompat.Builder(
            applicationContext,
            NotificationChannels.CAPTURE_CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(payload.contactDisplayName)
            .setContentText(payload.messageText ?: "Новое личное сообщение")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)

        actions.forEach { (action, label) ->
            builder.addAction(0, label, actionPendingIntent(payload, action))
        }
        try {
            notificationManager.notify(notificationId(payload), builder.build())
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and the framework call.
        }
    }

    private fun actionPendingIntent(payload: CapturePayload, action: CaptureAction): PendingIntent {
        val intent = Intent(applicationContext, CaptureActionReceiver::class.java)
            .setAction(action.intentAction)
            .putCapturePayload(payload)
        return PendingIntent.getBroadcast(
            applicationContext,
            31 * notificationId(payload) + action.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        fun notificationId(payload: CapturePayload): Int =
            "${payload.sourcePackage}:${payload.conversationKey}".hashCode()
    }
}

internal val CaptureAction.intentAction: String
    get() = "app.replylater.android.capture.${name}"

internal fun Intent.putCapturePayload(payload: CapturePayload): Intent = apply {
    CapturePayloadCodec.encode(payload).forEach(::putExtra)
}

internal fun Intent.readCapturePayload(): CapturePayload? {
    val values = buildMap {
        listOf(
            CapturePayloadCodec.MESSENGER,
            CapturePayloadCodec.SOURCE_PACKAGE,
            CapturePayloadCodec.NOTIFICATION_KEY,
            CapturePayloadCodec.CONVERSATION_KEY,
            CapturePayloadCodec.CONTACT_NAME,
            CapturePayloadCodec.MESSAGE_TEXT,
            CapturePayloadCodec.RECEIVED_AT,
        ).forEach { key -> getStringExtra(key)?.let { put(key, it) } }
    }
    return CapturePayloadCodec.decode(values)
}
