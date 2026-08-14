package com.faceaccess.app.gesture

import com.faceaccess.app.metrics.FaceMetrics
import kotlin.math.abs

/**
 * Cong chong kich hoat nham: chuyen chuoi FaceMetrics thanh GestureEvent roi
 * rac, chi phat su kien khi cu chi duoc giu lien tuc >= holdDurationMs (phan
 * biet chop mat tu nhien ~100-400ms voi nham-giu chu y). Moi lan "giu lien
 * tuc" (khong tha ra) chi phat toi da MOT EYE_CLOSE_HOLD/HEAD_TURN_* - nguoi
 * dung phai tra ve trang thai trung tinh (mo mat/quay lai giua) roi lam lai
 * de kich hoat lan nua. Neu tiep tuc giu nham mat qua emergencyHoldDurationMs
 * se phat them EYE_CLOSE_LONG (dung khan cap). cooldownMs chi chan cac lan
 * phat qua sat nhau quanh ranh gioi tha/giu do rung tin hieu. Thuan Kotlin,
 * khong phu thuoc Android, goi onMetrics() theo dung thu tu thoi gian tang dan.
 *
 * Quy uoc dau yaw: yawDeg > 0 la quay dau sang PHAI, yawDeg < 0 la quay dau
 * sang TRAI (theo he truc cua FaceMetricsExtractor.eulerAnglesDegFromMatrix).
 * Quy uoc nay CHUA duoc doi chieu tren thiet bi that, xem canh bao trong
 * FaceMetricsExtractor.
 */
class GestureStateMachine(private var profile: CalibrationProfile = CalibrationProfile.DEFAULT) {

    private enum class Phase { IDLE, HEAD_TURNING, EYE_CLOSING }
    private enum class Direction { LEFT, RIGHT }

    private var phase = Phase.IDLE
    private var phaseStartMs = 0L
    private var headDirection: Direction? = null
    private var headFired = false
    private var eyeHoldFired = false
    private var eyeLongFired = false
    private var cooldownUntilMs = 0L
    private var lastFaceDetected = true

    fun updateProfile(newProfile: CalibrationProfile) {
        profile = newProfile
    }

    fun reset() {
        phase = Phase.IDLE
        headDirection = null
        headFired = false
        eyeHoldFired = false
        eyeLongFired = false
        cooldownUntilMs = 0L
        lastFaceDetected = true
    }

    fun onMetrics(metrics: FaceMetrics): GestureEvent? {
        if (!metrics.faceDetected) {
            val justLost = lastFaceDetected
            lastFaceDetected = false
            phase = Phase.IDLE
            headDirection = null
            return if (justLost) {
                GestureEvent(GestureType.FACE_LOST, metrics.elapsedMs, metrics, confidence = 1f)
            } else {
                null
            }
        }
        lastFaceDetected = true

        val eyesClosed = metrics.earLeft < profile.earClosedThreshold &&
            metrics.earRight < profile.earClosedThreshold
        val turningLeft = metrics.yawDeg <= -profile.yawLeftThresholdDeg
        val turningRight = metrics.yawDeg >= profile.yawRightThresholdDeg

        return when {
            eyesClosed -> handleEyeClosing(metrics)
            turningLeft && !turningRight -> handleHeadTurn(metrics, Direction.LEFT)
            turningRight && !turningLeft -> handleHeadTurn(metrics, Direction.RIGHT)
            else -> {
                phase = Phase.IDLE
                headDirection = null
                null
            }
        }
    }

    private fun handleEyeClosing(metrics: FaceMetrics): GestureEvent? {
        if (phase != Phase.EYE_CLOSING) {
            phase = Phase.EYE_CLOSING
            phaseStartMs = metrics.elapsedMs
            eyeHoldFired = false
            eyeLongFired = false
            return null
        }
        val heldMs = metrics.elapsedMs - phaseStartMs
        val inCooldown = metrics.elapsedMs < cooldownUntilMs
        val margin = (profile.earClosedThreshold - minOf(metrics.earLeft, metrics.earRight))
            .coerceAtLeast(0f)
        val confidence = (margin / profile.earClosedThreshold).coerceIn(0f, 1f)

        return when {
            heldMs >= profile.emergencyHoldDurationMs && !eyeLongFired && !inCooldown -> {
                eyeLongFired = true
                cooldownUntilMs = metrics.elapsedMs + profile.cooldownMs
                GestureEvent(GestureType.EYE_CLOSE_LONG, metrics.elapsedMs, metrics, confidence)
            }
            heldMs >= profile.holdDurationMs && !eyeHoldFired && !inCooldown -> {
                eyeHoldFired = true
                cooldownUntilMs = metrics.elapsedMs + profile.cooldownMs
                GestureEvent(GestureType.EYE_CLOSE_HOLD, metrics.elapsedMs, metrics, confidence)
            }
            else -> null
        }
    }

    private fun handleHeadTurn(metrics: FaceMetrics, direction: Direction): GestureEvent? {
        if (phase != Phase.HEAD_TURNING || headDirection != direction) {
            phase = Phase.HEAD_TURNING
            headDirection = direction
            phaseStartMs = metrics.elapsedMs
            headFired = false
            return null
        }
        val heldMs = metrics.elapsedMs - phaseStartMs
        val inCooldown = metrics.elapsedMs < cooldownUntilMs
        if (heldMs < profile.holdDurationMs || headFired || inCooldown) return null

        headFired = true
        cooldownUntilMs = metrics.elapsedMs + profile.cooldownMs
        val threshold = if (direction == Direction.LEFT) profile.yawLeftThresholdDeg else profile.yawRightThresholdDeg
        val confidence = ((abs(metrics.yawDeg) - threshold) / threshold).coerceIn(0f, 1f)
        val type = if (direction == Direction.LEFT) GestureType.HEAD_TURN_LEFT else GestureType.HEAD_TURN_RIGHT
        return GestureEvent(type, metrics.elapsedMs, metrics, confidence)
    }
}
