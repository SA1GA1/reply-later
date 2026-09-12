package app.replylater.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ReplyLaterColors = lightColorScheme(
    primary = ReplyRed,
    onPrimary = SurfaceWhite,
    primaryContainer = ReplyRedSoft,
    background = CanvasWhite,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    outlineVariant = DividerGray,
)

@Composable
fun ReplyLaterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ReplyLaterColors,
        typography = ReplyLaterTypography,
        content = content,
    )
}

