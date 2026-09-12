package app.replylater.android

import android.content.Context
import android.app.AlarmManager
import androidx.room.Room
import app.replylater.android.reminder.crypto.AndroidKeystoreMessageCipher
import app.replylater.android.reminder.data.ReplyLaterDatabase
import app.replylater.android.reminder.data.RoomReminderRepository
import app.replylater.android.reminder.domain.ReminderRepository
import app.replylater.android.reminder.domain.ReminderScheduler
import app.replylater.android.reminder.schedule.AlarmReminderScheduler
import app.replylater.android.reminder.schedule.ReminderReconciler

class AppGraph(context: Context) {
    private val applicationContext = context.applicationContext

    val database: ReplyLaterDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ReplyLaterDatabase::class.java,
            DATABASE_NAME,
        ).build()
    }

    private val messageCipher by lazy { AndroidKeystoreMessageCipher() }

    val reminderRepository: ReminderRepository by lazy {
        RoomReminderRepository(database.reminderDao(), messageCipher)
    }

    val reminderScheduler: ReminderScheduler by lazy {
        AlarmReminderScheduler(
            context = applicationContext,
            alarmManager = applicationContext.getSystemService(AlarmManager::class.java),
        )
    }

    val reminderReconciler: ReminderReconciler by lazy {
        ReminderReconciler(reminderRepository, reminderScheduler)
    }

    private companion object {
        const val DATABASE_NAME = "reply_later.db"
    }
}
