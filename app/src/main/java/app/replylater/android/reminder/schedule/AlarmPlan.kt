package app.replylater.android.reminder.schedule

enum class AlarmPrecision {
    EXACT,
    INEXACT,
}

data class AlarmPlan(
    val triggerAtEpochMillis: Long,
    val precision: AlarmPrecision,
)
