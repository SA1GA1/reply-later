package app.replylater.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.replylater.android.ui.ReplyLaterRoot
import app.replylater.android.ui.theme.ReplyLaterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReplyLaterTheme {
                ReplyLaterRoot()
            }
        }
    }
}

