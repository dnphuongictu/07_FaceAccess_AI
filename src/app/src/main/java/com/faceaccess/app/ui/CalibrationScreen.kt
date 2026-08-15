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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.faceaccess.app.R
import com.faceaccess.app.camera.CameraSourceManager
import com.faceaccess.app.camera.FaceLandmarkerHelper
import com.faceaccess.app.feedback.AudioFeedback
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

    fun reset() {
        openEarSum = 0f
        openEarCount = 0
        closedEarSum = 0f
        closedEarCount = 0
        yawLeftMaxAbs = 0f
        yawRightMaxAbs = 0f
    }
}

@Composable
fun CalibrationScreen(onDone: (CalibrationProfile) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var step by remember { mutableStateOf(CalibrationStep.LOOK_STRAIGHT) }
    var faceDetected by remember { mutableStateOf(false) }
    var liveMetrics by remember { mutableStateOf<FaceMetrics?>(null) }
    var calibrationError by remember { mutableStateOf(false) }
    val acc = remember { CalibrationAccumulator() }
    val previewView = remember { PreviewView(context) }
    val audioFeedback = remember { AudioFeedback(context) }

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
            audioFeedback.close()
        }
    }

    LaunchedEffect(step) {
        if (step == CalibrationStep.DONE) {
            val profile = buildProfileOrNull(acc)
            if (profile == null) {
                calibrationError = true
                audioFeedback.announceInstruction(context.getString(R.string.calibration_invalid))
            } else {
                onDone(profile)
            }
            return@LaunchedEffect
        }
        audioFeedback.announceInstruction(context.getString(stepInstructionRes(step)))
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
        if (calibrationError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.calibration_invalid),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = {
                if (calibrationError) {
                    acc.reset()
                    calibrationError = false
                    step = CalibrationStep.LOOK_STRAIGHT
                } else {
                    onCancel()
                }
            },
        ) {
            Text(
                stringResource(
                    if (calibrationError) R.string.calibration_retry else R.string.calibration_cancel,
                ),
            )
        }
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
    if (!earAvg.isFinite() || !metrics.yawDeg.isFinite()) return
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

private fun buildProfileOrNull(acc: CalibrationAccumulator): CalibrationProfile? {
    if (acc.openEarCount < MIN_CALIBRATION_SAMPLES ||
        acc.closedEarCount < MIN_CALIBRATION_SAMPLES ||
        acc.yawLeftMaxAbs < MIN_CALIBRATION_YAW_DEG ||
        acc.yawRightMaxAbs < MIN_CALIBRATION_YAW_DEG
    ) {
        return null
    }
    val openBaseline = acc.openEarSum / acc.openEarCount
    val closedAvg = acc.closedEarSum / acc.closedEarCount
    if (!openBaseline.isFinite() || !closedAvg.isFinite() ||
        openBaseline - closedAvg < MIN_EAR_SEPARATION
    ) {
        return null
    }
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

private const val MIN_CALIBRATION_SAMPLES = 10
private const val MIN_CALIBRATION_YAW_DEG = 12f
private const val MIN_EAR_SEPARATION = 0.02f

private fun nextStep(step: CalibrationStep): CalibrationStep = when (step) {
    CalibrationStep.LOOK_STRAIGHT -> CalibrationStep.CLOSE_EYES
    CalibrationStep.CLOSE_EYES -> CalibrationStep.TURN_LEFT
    CalibrationStep.TURN_LEFT -> CalibrationStep.TURN_RIGHT
    CalibrationStep.TURN_RIGHT -> CalibrationStep.DONE
    CalibrationStep.DONE -> CalibrationStep.DONE
}

@Composable
private fun stepInstruction(step: CalibrationStep): String = when (step) {
    else -> stringResource(stepInstructionRes(step))
}

private fun stepInstructionRes(step: CalibrationStep): Int = when (step) {
    CalibrationStep.LOOK_STRAIGHT -> R.string.calibration_step_look_straight
    CalibrationStep.CLOSE_EYES -> R.string.calibration_step_close_eyes
    CalibrationStep.TURN_LEFT -> R.string.calibration_step_turn_left
    CalibrationStep.TURN_RIGHT -> R.string.calibration_step_turn_right
    CalibrationStep.DONE -> R.string.calibration_done
}
