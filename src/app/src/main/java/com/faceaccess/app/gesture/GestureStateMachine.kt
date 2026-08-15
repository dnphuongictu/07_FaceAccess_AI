package com.faceaccess.app.gesture

import com.faceaccess.app.metrics.FaceMetrics
import kotlin.math.abs

/**
 * Cong chong kich hoat nham: chuyen chuoi FaceMetrics thanh GestureEvent roi
 * rac, chi phat su kien khi cu chi duoc giu lien tuc >= holdDurationMs (phan
 * biet chop mat tu nhien ~100-400ms voi nham-giu chu y). Moi lan "giu lien
 * tuc" (khong tha ra) chi phat toi da mot su kien. Nham mat ngan chi phat
 * EYE_CLOSE_HOLD khi mo mat lai; neu giu qua emergencyHoldDurationMs thi chi
 * phat EYE_CLOSE_LONG, khong phat Select truoc do. Nguoi dung phai tra ve
 * trung tinh truoc lan tiep theo. cooldownMs chan cac lan
 * phat qua sat nhau quanh ranh gioi tha/giu do rung tin hieu. Thuan Kotlin,
 * khong phu thuoc Android, goi onMetrics() theo dung thu tu thoi gian tang dan.
 *
 * Quy uoc dau yaw: yawDeg > 0 la quay dau sang PHAI, yawDeg < 0 la quay dau
 * sang TRAI (theo he truc cua FaceMetricsExtractor.eulerAnglesDegFromMatrix).
 * Quy uoc nay CHUA duoc doi chieu tren thiet bi that, xem canh bao trong
 * FaceMetricsExtractor.
 */
class GestureStateMachine(
    private var profile: CalibrationProfile = CalibrationProfile.DEFAULT,
    private val maxHeadRollDeg: Float = DEFAULT_MAX_HEAD_ROLL_DEG,
    private val faceRecoveryDurationMs: Long = DEFAULT_FACE_RECOVERY_DURATION_MS,
) {

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
    private var waitingForNeutral = false
    private var faceRecoveryStartMs: Long? = null
    private var requiredNeutralDurationMs = 0L

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
        waitingForNeutral = false
        faceRecoveryStartMs = null
        requiredNeutralDurationMs = 0L
    }

    fun onMetrics(metrics: FaceMetrics): GestureEvent? {
        if (!metrics.faceDetected) {
            val justLost = lastFaceDetected
            lastFaceDetected = false
            phase = Phase.IDLE
            headDirection = null
            requireNeutral(faceRecoveryDurationMs)
            return if (justLost) {
                GestureEvent(GestureType.FACE_LOST, metrics.elapsedMs, metrics, confidence = 1f)
            } else {
                null
            }
        }
        val eyesClosed = metrics.earLeft < profile.earClosedThreshold &&
            metrics.earRight < profile.earClosedThreshold
        val rollIsNeutral = abs(metrics.rollDeg) <= maxHeadRollDeg
        val turningLeft = rollIsNeutral && metrics.yawDeg <= -profile.yawLeftThresholdDeg
        val turningRight = rollIsNeutral && metrics.yawDeg >= profile.yawRightThresholdDeg
        val neutral = !eyesClosed && !turningLeft && !turningRight

        if (waitingForNeutral) {
            if (!neutral) {
                faceRecoveryStartMs = null
                return null
            }
            val recoveryStart = faceRecoveryStartMs
            if (recoveryStart == null) {
                faceRecoveryStartMs = metrics.elapsedMs
                return null
            }
            if (metrics.elapsedMs - recoveryStart < requiredNeutralDurationMs) return null
            waitingForNeutral = false
            faceRecoveryStartMs = null
            phase = Phase.IDLE
            // Chi coi la da tim lai khuon mat sau khi qua tron cua so phuc
            // hoi. Mot frame thay mat chap chon trong cua so nay khong duoc
            // lam FACE_LOST phat lap lai o frame ke tiep.
            lastFaceDetected = true
            return null
        }

        lastFaceDetected = true

        return when {
            eyesClosed -> handleEyeClosing(metrics)
            turningLeft && !turningRight -> handleHeadTurn(metrics, Direction.LEFT)
            turningRight && !turningLeft -> handleHeadTurn(metrics, Direction.RIGHT)
            else -> {
                val releasedEyeEvent = handleEyeRelease(metrics)
                phase = Phase.IDLE
                headDirection = null
                releasedEyeEvent
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
                requireNeutral(0L)
                GestureEvent(GestureType.EYE_CLOSE_LONG, metrics.elapsedMs, metrics, confidence)
            }
            else -> null
        }
    }

    /**
     * Select chi duoc phat khi nguoi dung MO MAT sau mot lan giu ngan. Nho do
     * mot lan giu dai de dung khan cap khong the thuc thi action dang highlight
     * truoc khi cham nguong emergency.
     */
    private fun handleEyeRelease(metrics: FaceMetrics): GestureEvent? {
        if (phase != Phase.EYE_CLOSING || eyeLongFired || eyeHoldFired) return null
        val heldMs = metrics.elapsedMs - phaseStartMs
        val inCooldown = metrics.elapsedMs < cooldownUntilMs
        if (heldMs >= profile.emergencyHoldDurationMs && !inCooldown) {
            eyeLongFired = true
            cooldownUntilMs = metrics.elapsedMs + profile.cooldownMs
            requireNeutral(0L)
            return GestureEvent(GestureType.EYE_CLOSE_LONG, metrics.elapsedMs, metrics, confidence = 1f)
        }
        if (heldMs < profile.holdDurationMs || inCooldown) return null

        eyeHoldFired = true
        cooldownUntilMs = metrics.elapsedMs + profile.cooldownMs
        val closedMargin = (profile.earOpenBaseline - profile.earClosedThreshold)
            .coerceAtLeast(0.01f)
        val confidence = (heldMs.toFloat() / profile.emergencyHoldDurationMs.toFloat())
            .coerceIn(0f, 1f) * (closedMargin / profile.earOpenBaseline.coerceAtLeast(0.01f))
                .coerceIn(0f, 1f)
        return GestureEvent(GestureType.EYE_CLOSE_HOLD, metrics.elapsedMs, metrics, confidence)
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
        requireNeutral(0L)
        val threshold = if (direction == Direction.LEFT) profile.yawLeftThresholdDeg else profile.yawRightThresholdDeg
        val confidence = ((abs(metrics.yawDeg) - threshold) / threshold).coerceIn(0f, 1f)
        val type = if (direction == Direction.LEFT) GestureType.HEAD_TURN_LEFT else GestureType.HEAD_TURN_RIGHT
        return GestureEvent(type, metrics.elapsedMs, metrics, confidence)
    }

    private fun requireNeutral(durationMs: Long) {
        waitingForNeutral = true
        faceRecoveryStartMs = null
        requiredNeutralDurationMs = durationMs
    }

    companion object {
        const val DEFAULT_MAX_HEAD_ROLL_DEG = 12f
        const val DEFAULT_FACE_RECOVERY_DURATION_MS = 1_000L
    }
}
