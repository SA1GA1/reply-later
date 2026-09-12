package app.replylater.android.reminder.schedule

import android.app.AlarmManager
import android.os.Build

data class AlarmCapability(
    val sdkInt: Int,
    val canScheduleExact: Boolean,
)

fun AlarmManager.currentCapability(): AlarmCapability = AlarmCapability(
    sdkInt = Build.VERSION.SDK_INT,
    canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms(),
)
