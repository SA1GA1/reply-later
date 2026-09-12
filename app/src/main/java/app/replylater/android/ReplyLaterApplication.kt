package app.replylater.android

import android.app.Application

class ReplyLaterApplication : Application() {
    val graph: AppGraph by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppGraph(this)
    }
}
