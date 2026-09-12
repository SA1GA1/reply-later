package app.replylater.android.capture.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.replylater.android.ui.theme.SurfaceWhite
import app.replylater.android.ui.theme.TextSecondary

@Composable
fun CaptureInspectorPanel(modifier: Modifier = Modifier) {
    val entries = CaptureInspectorStore.entries.value
    val showContent = CaptureInspectorStore.showContent.value

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Диагностика уведомлений", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Показывать содержимое в этой сессии",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Switch(
                    checked = showContent,
                    onCheckedChange = CaptureInspectorStore::setShowContent,
                )
            }

            if (entries.isEmpty()) {
                Text(
                    text = "Событий пока нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            } else {
                entries.take(DISPLAYED_ENTRIES).forEach { entry ->
                    InspectorEntryRow(entry = entry, showContent = showContent)
                }
            }
        }
    }
}

@Composable
private fun InspectorEntryRow(entry: InspectorEntry, showContent: Boolean) {
    val decision = if (entry.accepted) "Личное сообщение" else "Отклонено: ${entry.rejectionReason}"
    val messenger = entry.messenger?.name ?: "UNKNOWN"
    val structure = "title=${entry.hadTitle}, text=${entry.hadText}, " +
        "conversation=${entry.hadConversationTitle}, messages=${entry.messageCount}"

    Column {
        Text("$messenger · $decision", style = MaterialTheme.typography.bodyMedium)
        Text(structure, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        if (showContent && entry.accepted) {
            Text(
                text = listOfNotNull(entry.contactDisplayName, entry.messagePreview).joinToString(": "),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

private const val DISPLAYED_ENTRIES = 5

