package app.replylater.android.reminder.domain

interface ReminderScheduler {
    fun schedule(reminderId: String, remindAtEpochMillis: Long)

    fun cancel(reminderId: String)
}
