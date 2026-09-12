package app.replylater.android.capture.companion

enum class CaptureAction {
    EXPAND,
    SAVE_30_MINUTES,
    SAVE_60_MINUTES,
    MORE,
}

sealed interface CaptureActionResult {
    data object Expanded : CaptureActionResult
    data class Saved(val reminderId: String) : CaptureActionResult
    data class OpenCustom(val payload: CapturePayload) : CaptureActionResult
}

fun interface CaptureReminderCreator {
    suspend fun create(payload: CapturePayload, remindAtEpochMillis: Long): String
}

class CaptureActionHandler(
    private val reminderCreator: CaptureReminderCreator,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun handle(action: CaptureAction, payload: CapturePayload): CaptureActionResult = when (action) {
        CaptureAction.EXPAND -> CaptureActionResult.Expanded
        CaptureAction.MORE -> CaptureActionResult.OpenCustom(payload)
        CaptureAction.SAVE_30_MINUTES -> save(payload, THIRTY_MINUTES_MILLIS)
        CaptureAction.SAVE_60_MINUTES -> save(payload, SIXTY_MINUTES_MILLIS)
    }

    private suspend fun save(
        payload: CapturePayload,
        delayMillis: Long,
    ): CaptureActionResult.Saved = CaptureActionResult.Saved(
        reminderCreator.create(payload, nowEpochMillis() + delayMillis),
    )

    private companion object {
        const val THIRTY_MINUTES_MILLIS = 30L * 60L * 1_000L
        const val SIXTY_MINUTES_MILLIS = 60L * 60L * 1_000L
    }
}
