package app.replylater.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val CAPTURE_CHANNEL_ID = "capture_candidates_v1"
    const val REMINDER_CHANNEL_ID = "scheduled_reminders_v1"

    fun ensureCaptureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CAPTURE_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CAPTURE_CHANNEL_ID,
                "Предложения ответить позже",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Тихие действия для выбранных личных сообщений"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            },
        )
    }
}
