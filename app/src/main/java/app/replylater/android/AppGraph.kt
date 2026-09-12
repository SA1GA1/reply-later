package app.replylater.android

import android.content.Context
import androidx.room.Room
import app.replylater.android.reminder.crypto.AndroidKeystoreMessageCipher
import app.replylater.android.reminder.data.ReplyLaterDatabase
import app.replylater.android.reminder.data.RoomReminderRepository
import app.replylater.android.reminder.domain.ReminderRepository

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

    private companion object {
        const val DATABASE_NAME = "reply_later.db"
    }
}
