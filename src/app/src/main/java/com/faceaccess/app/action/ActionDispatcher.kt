package com.faceaccess.app.action

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import com.faceaccess.app.access.FaceAccessAccessibilityService
import com.faceaccess.app.overlay.ScanAction

/**
 * Thuc thi ScanAction dang duoc highlight khi nguoi dung "chon". Back/Home
 * can FaceAccessAccessibilityService da duoc nguoi dung bat trong Cai dat
 * trợ nang; neu chua bat thi tra ve BLOCKED_NO_ACCESSIBILITY_SERVICE thay vi
 * lam gi do khong an toan.
 */
class ActionDispatcher(
    private val context: Context,
    private val onEmergencyStop: () -> Unit,
) {

    fun dispatch(action: ScanAction): ActionResult = when (action) {
        ScanAction.Back -> performGlobalAction(GLOBAL_ACTION_BACK)
        ScanAction.Home -> performGlobalAction(GLOBAL_ACTION_HOME)
        ScanAction.MediaPlayPause -> dispatchMediaPlayPause()
        ScanAction.EmergencyStop -> {
            onEmergencyStop()
            ActionResult.EXECUTED
        }
        is ScanAction.OpenApp -> dispatchOpenApp(action.packageName)
    }

    private fun performGlobalAction(globalAction: Int): ActionResult {
        val service = FaceAccessAccessibilityService.instance
            ?: return ActionResult.BLOCKED_NO_ACCESSIBILITY_SERVICE
        return if (service.performGlobalAction(globalAction)) {
            ActionResult.EXECUTED
        } else {
            ActionResult.IGNORED
        }
    }

    private fun dispatchMediaPlayPause(): ActionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionResult.IGNORED
        val eventTime = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0),
        )
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0),
        )
        return ActionResult.EXECUTED
    }

    private fun dispatchOpenApp(packageName: String): ActionResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return ActionResult.IGNORED
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return ActionResult.EXECUTED
    }
}
