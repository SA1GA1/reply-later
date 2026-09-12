package app.replylater.android.capture.debug

import app.replylater.android.capture.model.Messenger
import app.replylater.android.capture.parser.RejectReason

data class InspectorEntry(
    val timestampEpochMillis: Long,
    val messenger: Messenger?,
    val accepted: Boolean,
    val rejectionReason: RejectReason?,
    val hadTitle: Boolean,
    val hadText: Boolean,
    val hadConversationTitle: Boolean,
    val messageCount: Int,
    val contactDisplayName: String?,
    val messagePreview: String?,
)

