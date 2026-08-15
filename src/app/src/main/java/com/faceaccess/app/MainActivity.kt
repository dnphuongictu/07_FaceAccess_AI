package com.faceaccess.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.faceaccess.app.data.CalibrationStore
import com.faceaccess.app.overlay.FaceAccessOverlayService
import com.faceaccess.app.permissions.PermissionState
import com.faceaccess.app.ui.CalibrationScreen
import com.faceaccess.app.ui.OnboardingScreen
import com.faceaccess.app.ui.PinnedAppsScreen
import com.faceaccess.app.ui.theme.FaceAccessTheme
import kotlinx.coroutines.launch

private enum class AppScreen { ONBOARDING, CALIBRATION, PINNED_APPS }

class MainActivity : ComponentActivity() {

    private var screen by mutableStateOf(AppScreen.ONBOARDING)
    private var cameraGranted by mutableStateOf(false)
    private var overlayGranted by mutableStateOf(false)
    private var accessibilityGranted by mutableStateOf(false)
    private var calibrationCompleted by mutableStateOf(false)

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraGranted = granted }

    private val overlaySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { overlayGranted = PermissionState.hasOverlayPermission(this) }

    private val accessibilitySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { accessibilityGranted = PermissionState.isAccessibilityServiceEnabled(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionState()
        lifecycleScope.launch {
            CalibrationStore(this@MainActivity).isCalibratedFlow.collect { completed ->
                calibrationCompleted = completed
            }
        }

        setContent {
            FaceAccessTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (screen) {
                        AppScreen.ONBOARDING -> OnboardingScreen(
                            cameraGranted = cameraGranted,
                            overlayGranted = overlayGranted,
                            accessibilityGranted = accessibilityGranted,
                            calibrationCompleted = calibrationCompleted,
                            onRequestCamera = { requestCameraPermission.launch(Manifest.permission.CAMERA) },
                            onRequestOverlay = { overlaySettingsLauncher.launch(overlaySettingsIntent()) },
                            onRequestAccessibility = {
                                accessibilitySettingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onCalibrateClick = { screen = AppScreen.CALIBRATION },
                            onPinnedAppsClick = { screen = AppScreen.PINNED_APPS },
                            onStartClick = {
                                if (calibrationCompleted) {
                                    FaceAccessOverlayService.start(this@MainActivity)
                                    finish()
                                }
                            },
                        )

                        AppScreen.CALIBRATION -> CalibrationScreen(
                            onDone = { profile ->
                                lifecycleScope.launch { CalibrationStore(this@MainActivity).save(profile) }
                                screen = AppScreen.ONBOARDING
                            },
                            onCancel = { screen = AppScreen.ONBOARDING },
                        )

                        AppScreen.PINNED_APPS -> PinnedAppsScreen(
                            onDone = { screen = AppScreen.ONBOARDING },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        cameraGranted = PermissionState.hasCameraPermission(this)
        overlayGranted = PermissionState.hasOverlayPermission(this)
        accessibilityGranted = PermissionState.isAccessibilityServiceEnabled(this)
    }

    private fun overlaySettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
}
