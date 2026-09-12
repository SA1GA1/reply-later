package app.replylater.android.reminder.schedule

import app.replylater.android.reminder.domain.ReminderRepository
import app.replylater.android.reminder.domain.ReminderScheduler

class ReminderReconciler(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun reconcile(): List<String> {
        val now = nowEpochMillis()
        val (overdue, future) = repository.unfinished().partition {
            it.remindAtEpochMillis <= now
        }
        future.forEach { scheduler.schedule(it.id, it.remindAtEpochMillis) }
        return overdue.map { it.id }
    }
}
