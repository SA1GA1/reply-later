package app.replylater.android.notification

object NotificationPostingPolicy {
    fun canPost(sdkInt: Int, permissionGranted: Boolean): Boolean =
        sdkInt < 33 || permissionGranted
}
