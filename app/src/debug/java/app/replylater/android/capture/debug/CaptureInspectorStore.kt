package app.replylater.android.capture.debug

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

object CaptureInspectorStore {
    private val mutableEntries = mutableStateOf<List<InspectorEntry>>(emptyList())
    private val mutableShowContent = mutableStateOf(false)

    val entries: State<List<InspectorEntry>> = mutableEntries
    val showContent: State<Boolean> = mutableShowContent

    fun record(entry: InspectorEntry) {
        mutableEntries.value = (listOf(entry) + mutableEntries.value).take(MAX_ENTRIES)
    }

    fun setShowContent(show: Boolean) {
        mutableShowContent.value = show
    }

    private const val MAX_ENTRIES = 50
}

