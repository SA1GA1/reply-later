package app.replylater.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.replylater.android.ui.theme.CanvasWhite
import app.replylater.android.capture.debug.CaptureInspectorPanel
import app.replylater.android.ui.theme.ReplyLaterTheme
import app.replylater.android.ui.theme.ReplyRed
import app.replylater.android.ui.theme.SurfaceWhite
import app.replylater.android.ui.theme.TextPrimary
import app.replylater.android.ui.theme.TextSecondary
import java.time.LocalDate

@Composable
fun ReplyLaterRoot(
    today: LocalDate = LocalDate.now(),
    onStartSetup: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Text(
            text = "Reply Later",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = formatHomeDate(today),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )

        Spacer(Modifier.height(40.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = TextPrimary.copy(alpha = 0.08f),
                    spotColor = TextPrimary.copy(alpha = 0.08f),
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceWhite.copy(alpha = 0.94f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Ответьте тогда, когда удобно",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Сохраняйте только выбранные сообщения из Telegram и WhatsApp и возвращайтесь к ним по напоминанию.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onStartSetup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ReplyRed),
                ) {
                    Text("Настроить доступ")
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        CaptureInspectorPanel()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                text = "Сообщения обрабатываются на устройстве",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReplyLaterRootPreview() {
    ReplyLaterTheme {
        ReplyLaterRoot(today = LocalDate.of(2026, 9, 12))
    }
}
