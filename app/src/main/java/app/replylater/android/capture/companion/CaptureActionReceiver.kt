package app.replylater.android.capture.companion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.replylater.android.MainActivity
import app.replylater.android.ReplyLaterApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CaptureActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = CaptureAction.entries.firstOrNull { it.intentAction == intent.action } ?: return
        val payload = intent.readCapturePayload() ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val graph = (context.applicationContext as ReplyLaterApplication).graph
                when (graph.captureActionHandler.handle(action, payload)) {
                    CaptureActionResult.Expanded -> graph.companionNotificationPublisher
                        .publishExpanded(payload)

                    is CaptureActionResult.Saved -> graph.companionNotificationPublisher
                        .cancel(payload)

                    is CaptureActionResult.OpenCustom -> openCustomTime(context, payload)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun openCustomTime(context: Context, payload: CapturePayload) {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_CUSTOM_TIME)
                .putCapturePayload(payload)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    companion object {
        const val ACTION_CUSTOM_TIME = "app.replylater.android.action.CUSTOM_TIME"
    }
}
