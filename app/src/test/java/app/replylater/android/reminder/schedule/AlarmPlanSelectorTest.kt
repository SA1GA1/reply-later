package app.replylater.android.reminder.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmPlanSelectorTest {
    @Test
    fun api31WithPermissionUsesExactAlarm() {
        assertEquals(
            AlarmPlan(2_000L, AlarmPrecision.EXACT),
            AlarmPlanSelector.select(
                sdkInt = 31,
                canScheduleExact = true,
                nowEpochMillis = 1_000L,
                targetEpochMillis = 2_000L,
            ),
        )
    }

    @Test
    fun api31WithoutPermissionUsesInexactAlarm() {
        assertEquals(
            AlarmPlan(2_000L, AlarmPrecision.INEXACT),
            AlarmPlanSelector.select(31, false, 1_000L, 2_000L),
        )
    }

    @Test
    fun api30UsesExactAlarmWithoutSpecialAccess() {
        assertEquals(
            AlarmPlan(2_000L, AlarmPrecision.EXACT),
            AlarmPlanSelector.select(30, false, 1_000L, 2_000L),
        )
    }

    @Test
    fun pastTargetBecomesImmediateInexactAlarm() {
        assertEquals(
            AlarmPlan(1_000L, AlarmPrecision.INEXACT),
            AlarmPlanSelector.select(35, true, 1_000L, 999L),
        )
    }
}
