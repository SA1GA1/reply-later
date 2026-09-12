package app.replylater.android.reminder.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.replylater.android.reminder.domain.ReminderScheduler

class AlarmReminderScheduler(
    context: Context,
    private val alarmManager: AlarmManager,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) : ReminderScheduler {
    private val applicationContext = context.applicationContext

    override fun schedule(reminderId: String, remindAtEpochMillis: Long) {
        val capability = alarmManager.currentCapability()
        val plan = AlarmPlanSelector.select(
            sdkInt = capability.sdkInt,
            canScheduleExact = capability.canScheduleExact,
            nowEpochMillis = nowEpochMillis(),
            targetEpochMillis = remindAtEpochMillis,
        )
        val operation = checkNotNull(
            alarmPendingIntent(reminderId, PendingIntent.FLAG_UPDATE_CURRENT),
        )
        when (plan.precision) {
            AlarmPrecision.EXACT -> alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                plan.triggerAtEpochMillis,
                operation,
            )

            AlarmPrecision.INEXACT -> alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                plan.triggerAtEpochMillis,
                operation,
            )
        }
    }

    override fun cancel(reminderId: String) {
        val operation = alarmPendingIntent(reminderId, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(operation)
        operation.cancel()
    }

    private fun alarmPendingIntent(reminderId: String, modeFlag: Int): PendingIntent? {
        val intent = Intent(ACTION_DELIVER_REMINDER)
            .setClassName(applicationContext, REMINDER_ALARM_RECEIVER)
            .putExtra(EXTRA_REMINDER_ID, reminderId)
        return PendingIntent.getBroadcast(
            applicationContext,
            reminderId.hashCode(),
            intent,
            modeFlag or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_DELIVER_REMINDER = "app.replylater.android.action.DELIVER_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        private const val REMINDER_ALARM_RECEIVER =
            "app.replylater.android.reminder.schedule.ReminderAlarmReceiver"
    }
}
