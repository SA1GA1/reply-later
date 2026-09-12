package app.replylater.android.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPostingPolicyTest {
    @Test
    fun api33RequiresGrantedRuntimePermission() {
        assertFalse(NotificationPostingPolicy.canPost(sdkInt = 33, permissionGranted = false))
        assertTrue(NotificationPostingPolicy.canPost(sdkInt = 33, permissionGranted = true))
    }

    @Test
    fun api32DoesNotRequireRuntimePermission() {
        assertTrue(NotificationPostingPolicy.canPost(sdkInt = 32, permissionGranted = false))
    }
}
