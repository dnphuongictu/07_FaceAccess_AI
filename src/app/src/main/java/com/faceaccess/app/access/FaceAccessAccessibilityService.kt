package com.faceaccess.app.access

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Chi dung de phat performGlobalAction (BACK/HOME) thay cho nguoi dung.
 * KHONG doc noi dung man hinh cua app khac trong pham vi MVP nay - xem
 * res/xml/accessibility_service_config.xml (canRetrieveWindowContent=false).
 */
class FaceAccessAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Co y bo trong: khong xu ly noi dung su kien.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    companion object {
        var instance: FaceAccessAccessibilityService? = null
            private set

        val isConnected: Boolean get() = instance != null
    }
}
