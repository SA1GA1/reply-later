package app.replylater.android.reminder.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReminderEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ReplyLaterDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}
