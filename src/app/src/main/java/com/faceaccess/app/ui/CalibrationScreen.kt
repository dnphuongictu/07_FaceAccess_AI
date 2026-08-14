package com.faceaccess.app.ui

import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.faceaccess.app.R
import com.faceaccess.app.camera.CameraSourceManager
import com.faceaccess.app.camera.FaceLandmarkerHelper
import com.faceaccess.app.gesture.CalibrationProfile
import com.faceaccess.app.metrics.FaceMetrics
import kotlinx.coroutines.delay

private enum class CalibrationStep { LOOK_STRAIGHT, CLOSE_EYES, TURN_LEFT, TURN_RIGHT, DONE }

private const val STEP_DURATION_MS = 8000L
private const val TAG = "CalibrationScreen"

/**
 * Cong don so lieu tho tu cac buoc hieu chinh. Khong phai Compose State co y:
 * ghi tu luong callback cua MediaPipe, doc lai mot lan khi buoc DONE - chap
 * nhan do chinh xac tuong doi (khong critical-safety) cho muc dich hieu chinh.
 */
private class CalibrationAccumulator {
    var openEarSum = 0f
    var openEarCount = 0
    var closedEarSum = 0f
    var closedEarCount = 0
    var yawLeftMaxAbs = 0f
    var yawRightMaxAbs = 0f
}

@Composable
fun CalibrationScreen(onDone: (CalibrationProfile) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var step by remember { mutableStateOf(CalibrationStep.LOOK_STRAIGHT) }
    var faceDetected by remember { mutableStateOf(false) }
    var liveMetrics by remember { mutableStateOf<FaceMetrics?>(null) }
    val acc = remember { CalibrationAccumulator() }
    val previewView = remember { PreviewView(context) }

    val faceLandmarkerHelper = remember {
        FaceLandmarkerHelper(
            context = context,
            listener = object : FaceLandmarkerHelper.Listener {
                override fun onFaceMetrics(metrics: FaceMetrics) {
                    faceDetected = metrics.faceDetected
                    liveMetrics = metrics
                    if (!metrics.faceDetected) return
                    accumulate(step, metrics, acc)
                }

                override fun onError(message: String) {
                    Log.e(TAG, message)
                }
            },
        )
    }
    val cameraSourceManager = remember { CameraSourceManager(context, faceLandmarkerHelper) }

    DisposableEffect(Unit) {
        faceLandmarkerHelper.setup()
        cameraSourceManager.start(lifecycleOwner, previewView.surfaceProvider)
        onDispose {
            cameraSourceManager.stop()
            faceLandmarkerHelper.close()
        }
    }

    LaunchedEffect(step) {
        if (step == CalibrationStep.DONE) {
            onDone(buildProfile(acc))
            return@LaunchedEffect
        }
        delay(STEP_DURATION_MS)
        step = nextStep(step)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.onboarding_calibration_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stepInstruction(step), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (faceDetected) stringResource(R.string.status_face_ok) else stringResource(R.string.status_face_lost),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = liveMetricsText(liveMetrics), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onCancel) { Text(stringResource(R.string.calibration_retry)) }
    }
}

/** Doc so lieu song, phuc vu doi chieu truc quan camera <-> so lieu khi kiem thu tren thiet bi that. */
private fun liveMetricsText(metrics: FaceMetrics?): String {
    if (metrics == null || !metrics.faceDetected) return "EAR: -- / --   Yaw: --°   Pitch: --°"
    return "EAR: %.2f / %.2f   Yaw: %.1f°   Pitch: %.1f°".format(
        metrics.earLeft,
        metrics.earRight,
        metrics.yawDeg,
        metrics.pitchDeg,
    )
}

private fun accumulate(step: CalibrationStep, metrics: FaceMetrics, acc: CalibrationAccumulator) {
    val earAvg = (metrics.earLeft + metrics.earRight) / 2f
    when (step) {
        CalibrationStep.LOOK_STRAIGHT -> {
            acc.openEarSum += earAvg
            acc.openEarCount += 1
        }
        CalibrationStep.CLOSE_EYES -> {
            acc.closedEarSum += earAvg
            acc.closedEarCount += 1
        }
        CalibrationStep.TURN_LEFT -> {
            if (metrics.yawDeg < 0f) acc.yawLeftMaxAbs = maxOf(acc.yawLeftMaxAbs, -metrics.yawDeg)
        }
        CalibrationStep.TURN_RIGHT -> {
            if (metrics.yawDeg > 0f) acc.yawRightMaxAbs = maxOf(acc.yawRightMaxAbs, metrics.yawDeg)
        }
        CalibrationStep.DONE -> Unit
    }
}

private fun buildProfile(acc: CalibrationAccumulator): CalibrationProfile {
    val openBaseline = if (acc.openEarCount > 0) acc.openEarSum / acc.openEarCount else CalibrationProfile.DEFAULT.earOpenBaseline
    val closedAvg = if (acc.closedEarCount > 0) acc.closedEarSum / acc.closedEarCount else CalibrationProfile.DEFAULT.earClosedThreshold
    // Nguong nam giua EAR mo va EAR nham do duoc, tranh chon sat mot trong hai cuc tri.
    val closedThreshold = ((openBaseline + closedAvg) / 2f).coerceIn(0.05f, (openBaseline - 0.02f).coerceAtLeast(0.06f))
    // Chi lay 60% goc quay toi da do duoc lam nguong, de cu chi kha thi nhung van tach biet voi rung lac dau nho.
    val yawLeft = (acc.yawLeftMaxAbs * 0.6f).coerceIn(12f, 45f)
    val yawRight = (acc.yawRightMaxAbs * 0.6f).coerceIn(12f, 45f)
    return CalibrationProfile(
        earOpenBaseline = openBaseline,
        earClosedThreshold = closedThreshold,
        yawLeftThresholdDeg = yawLeft,
        yawRightThresholdDeg = yawRight,
        holdDurationMs = CalibrationProfile.DEFAULT.holdDurationMs,
        cooldownMs = CalibrationProfile.DEFAULT.cooldownMs,
        emergencyHoldDurationMs = CalibrationProfile.DEFAULT.emergencyHoldDurationMs,
    )
}

private fun nextStep(step: CalibrationStep): CalibrationStep = when (step) {
    CalibrationStep.LOOK_STRAIGHT -> CalibrationStep.CLOSE_EYES
    CalibrationStep.CLOSE_EYES -> CalibrationStep.TURN_LEFT
    CalibrationStep.TURN_LEFT -> CalibrationStep.TURN_RIGHT
    CalibrationStep.TURN_RIGHT -> CalibrationStep.DONE
    CalibrationStep.DONE -> CalibrationStep.DONE
}

@Composable
private fun stepInstruction(step: CalibrationStep): String = when (step) {
    CalibrationStep.LOOK_STRAIGHT -> stringResource(R.string.calibration_step_look_straight)
    CalibrationStep.CLOSE_EYES -> stringResource(R.string.calibration_step_close_eyes)
    CalibrationStep.TURN_LEFT -> stringResource(R.string.calibration_step_turn_left)
    CalibrationStep.TURN_RIGHT -> stringResource(R.string.calibration_step_turn_right)
    CalibrationStep.DONE -> stringResource(R.string.calibration_done)
}
