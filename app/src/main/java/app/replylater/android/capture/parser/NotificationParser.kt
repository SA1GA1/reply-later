package app.replylater.android.capture.parser

import app.replylater.android.capture.model.DirectMessage
import app.replylater.android.capture.model.RawNotification

fun interface NotificationParser {
    fun parse(notification: RawNotification): ParseResult
}

sealed interface ParseResult {
    data class Accepted(val message: DirectMessage) : ParseResult

    data class Rejected(val reason: RejectReason) : ParseResult
}

enum class RejectReason {
    UNSUPPORTED_PACKAGE,
    NOT_A_MESSAGE,
    GROUP_CHAT,
    CHANNEL_OR_BOT,
    MISSING_CONTACT,
    AMBIGUOUS_CHAT,
}

