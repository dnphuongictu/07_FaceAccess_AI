package com.faceaccess.app.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.faceaccess.app.access.FaceAccessAccessibilityService

/** Kiem tra trang thai 3 quyen can cho onboarding: camera, overlay, trợ nang. */
object PermissionState {

    fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/${FaceAccessAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }
}
