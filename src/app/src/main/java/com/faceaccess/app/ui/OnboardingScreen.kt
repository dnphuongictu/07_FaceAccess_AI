package com.faceaccess.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.faceaccess.app.R

/**
 * Man hinh thiet lap: cap 3 quyen bat buoc, hieu chinh, ghim ung dung, roi
 * moi cho phep bat dau FaceAccessOverlayService. Khong tu bat quyen he thong
 * duoc - chi dieu huong toi dung man hinh Settings, nguoi dung phai tu cap.
 */
@Composable
fun OnboardingScreen(
    cameraGranted: Boolean,
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    calibrationCompleted: Boolean,
    onRequestCamera: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onCalibrateClick: () -> Unit,
    onPinnedAppsClick: () -> Unit,
    onStartClick: () -> Unit,
) {
    val readyToStart = cameraGranted && overlayGranted && accessibilityGranted && calibrationCompleted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)

        PermissionStepCard(
            title = stringResource(R.string.onboarding_camera_title),
            description = stringResource(R.string.onboarding_camera_desc),
            granted = cameraGranted,
            onClick = onRequestCamera,
        )
        PermissionStepCard(
            title = stringResource(R.string.onboarding_overlay_title),
            description = stringResource(R.string.onboarding_overlay_desc),
            granted = overlayGranted,
            onClick = onRequestOverlay,
        )
        PermissionStepCard(
            title = stringResource(R.string.onboarding_accessibility_title),
            description = stringResource(R.string.onboarding_accessibility_desc),
            granted = accessibilityGranted,
            onClick = onRequestAccessibility,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.onboarding_calibration_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.onboarding_calibration_desc), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onCalibrateClick, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (calibrationCompleted) {
                            stringResource(R.string.onboarding_recalibrate_button)
                        } else {
                            stringResource(R.string.onboarding_calibrate_button)
                        },
                    )
                }
                Text(
                    text = if (calibrationCompleted) {
                        stringResource(R.string.onboarding_calibration_ready)
                    } else {
                        stringResource(R.string.onboarding_calibration_required)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.pinned_apps_title), style = MaterialTheme.typography.titleLarge)
                Button(onClick = onPinnedAppsClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.pinned_apps_add))
                }
            }
        }

        Button(
            onClick = onStartClick,
            enabled = readyToStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_start_button))
        }
    }
}

@Composable
private fun PermissionStepCard(
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onClick, enabled = !granted, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (granted) {
                        stringResource(R.string.onboarding_granted)
                    } else {
                        stringResource(R.string.onboarding_grant_button)
                    },
                )
            }
        }
    }
}
