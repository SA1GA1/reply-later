package app.replylater.android.reminder.schedule

object AlarmPlanSelector {
    fun select(
        sdkInt: Int,
        canScheduleExact: Boolean,
        nowEpochMillis: Long,
        targetEpochMillis: Long,
    ): AlarmPlan {
        if (targetEpochMillis <= nowEpochMillis) {
            return AlarmPlan(nowEpochMillis, AlarmPrecision.INEXACT)
        }
        val exact = sdkInt < 31 || canScheduleExact
        return AlarmPlan(
            triggerAtEpochMillis = targetEpochMillis,
            precision = if (exact) AlarmPrecision.EXACT else AlarmPrecision.INEXACT,
        )
    }
}
