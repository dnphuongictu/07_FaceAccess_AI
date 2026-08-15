package com.faceaccess.app.gesture

import com.faceaccess.app.metrics.FaceMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureStateMachineTest {

    private val profile = CalibrationProfile(
        earOpenBaseline = 0.3f,
        earClosedThreshold = 0.15f,
        yawLeftThresholdDeg = 20f,
        yawRightThresholdDeg = 20f,
        holdDurationMs = 300L,
        cooldownMs = 500L,
        emergencyHoldDurationMs = 1000L,
    )

    private fun metrics(
        elapsedMs: Long,
        earLeft: Float = 0.3f,
        earRight: Float = 0.3f,
        yawDeg: Float = 0f,
        rollDeg: Float = 0f,
        faceDetected: Boolean = true,
    ) = FaceMetrics(
        elapsedMs = elapsedMs,
        faceDetected = faceDetected,
        earLeft = earLeft,
        earRight = earRight,
        mar = 0f,
        yawDeg = yawDeg,
        pitchDeg = 0f,
        rollDeg = rollDeg,
    )

    @Test
    fun `natural blink shorter than hold duration does not trigger select`() {
        val sm = GestureStateMachine(profile)

        assertNull(sm.onMetrics(metrics(elapsedMs = 0, earLeft = 0.05f, earRight = 0.05f)))
        assertNull(sm.onMetrics(metrics(elapsedMs = 150, earLeft = 0.05f, earRight = 0.05f)))
        val released = sm.onMetrics(metrics(elapsedMs = 180, earLeft = 0.3f, earRight = 0.3f))

        assertNull(released)
    }

    @Test
    fun `sustained eye closure fires select only after eyes reopen`() {
        val sm = GestureStateMachine(profile)

        assertNull(sm.onMetrics(metrics(elapsedMs = 0, earLeft = 0.05f, earRight = 0.05f)))
        assertNull(sm.onMetrics(metrics(elapsedMs = 310, earLeft = 0.05f, earRight = 0.05f)))
        val event = sm.onMetrics(metrics(elapsedMs = 320))

        assertEquals(GestureType.EYE_CLOSE_HOLD, event?.type)
    }

    @Test
    fun `holding closed eyes without releasing does not fire select`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, earLeft = 0.05f, earRight = 0.05f))
        val first = sm.onMetrics(metrics(elapsedMs = 310, earLeft = 0.05f, earRight = 0.05f))
        val stillHeld = sm.onMetrics(metrics(elapsedMs = 500, earLeft = 0.05f, earRight = 0.05f))

        assertNull(first)
        assertNull(stillHeld)
    }

    @Test
    fun `emergency eye hold never fires select first`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, earLeft = 0.05f, earRight = 0.05f))
        val hold = sm.onMetrics(metrics(elapsedMs = 310, earLeft = 0.05f, earRight = 0.05f))
        val long = sm.onMetrics(metrics(elapsedMs = 1010, earLeft = 0.05f, earRight = 0.05f))

        assertNull(hold)
        assertEquals(GestureType.EYE_CLOSE_LONG, long?.type)
        assertNull(sm.onMetrics(metrics(elapsedMs = 1100)))
    }

    @Test
    fun `opening eyes after emergency threshold still emits only emergency`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, earLeft = 0.05f, earRight = 0.05f))
        val event = sm.onMetrics(metrics(elapsedMs = 1_100))

        assertEquals(GestureType.EYE_CLOSE_LONG, event?.type)
    }

    @Test
    fun `head turn left held past hold duration fires once then blocks repeat while held`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, yawDeg = -25f))
        val fired = sm.onMetrics(metrics(elapsedMs = 310, yawDeg = -25f))
        val repeat = sm.onMetrics(metrics(elapsedMs = 320, yawDeg = -25f))

        assertEquals(GestureType.HEAD_TURN_LEFT, fired?.type)
        assertNull(repeat)
    }

    @Test
    fun `head turn right recognized with opposite sign convention`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, yawDeg = 25f))
        val fired = sm.onMetrics(metrics(elapsedMs = 310, yawDeg = 25f))

        assertEquals(GestureType.HEAD_TURN_RIGHT, fired?.type)
    }

    @Test
    fun `releasing to neutral and turning again fires a second HEAD_TURN_LEFT after cooldown`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, yawDeg = -25f))
        val firstFire = sm.onMetrics(metrics(elapsedMs = 310, yawDeg = -25f))
        sm.onMetrics(metrics(elapsedMs = 330, yawDeg = 0f)) // ve trung tinh
        sm.onMetrics(metrics(elapsedMs = 340, yawDeg = 0f))
        sm.onMetrics(metrics(elapsedMs = 820, yawDeg = -25f)) // qua cooldown (310+500=810), bat dau lai
        val secondFire = sm.onMetrics(metrics(elapsedMs = 1130, yawDeg = -25f))

        assertEquals(GestureType.HEAD_TURN_LEFT, firstFire?.type)
        assertEquals(GestureType.HEAD_TURN_LEFT, secondFire?.type)
    }

    @Test
    fun `losing face emits FACE_LOST once on the transition edge only`() {
        val sm = GestureStateMachine(profile)

        assertNull(sm.onMetrics(metrics(elapsedMs = 0)))
        val lost = sm.onMetrics(metrics(elapsedMs = 50, faceDetected = false))
        val stillLost = sm.onMetrics(metrics(elapsedMs = 100, faceDetected = false))

        assertEquals(GestureType.FACE_LOST, lost?.type)
        assertNull(stillLost)
    }

    @Test
    fun `losing face mid head turn cancels the in-progress gesture`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, yawDeg = -25f))
        sm.onMetrics(metrics(elapsedMs = 100, faceDetected = false))
        val resumed = sm.onMetrics(metrics(elapsedMs = 150, yawDeg = -25f))

        // Phai bat dau lai tu dau (transition), chua du 300ms nen khong fire.
        assertNull(resumed)
    }

    @Test
    fun `face reacquisition requires stable neutral interval`() {
        val sm = GestureStateMachine(profile, faceRecoveryDurationMs = 1_000L)

        sm.onMetrics(metrics(elapsedMs = 0, faceDetected = false))
        assertNull(sm.onMetrics(metrics(elapsedMs = 100, yawDeg = -25f)))
        assertNull(sm.onMetrics(metrics(elapsedMs = 200)))
        assertNull(sm.onMetrics(metrics(elapsedMs = 1_199)))
        assertNull(sm.onMetrics(metrics(elapsedMs = 1_200)))
        assertNull(sm.onMetrics(metrics(elapsedMs = 1_210, yawDeg = -25f)))
        val event = sm.onMetrics(metrics(elapsedMs = 1_520, yawDeg = -25f))

        assertEquals(GestureType.HEAD_TURN_LEFT, event?.type)
    }

    @Test
    fun `transient face during recovery does not repeat face lost warning`() {
        val sm = GestureStateMachine(profile, faceRecoveryDurationMs = 1_000L)

        val firstLost = sm.onMetrics(metrics(elapsedMs = 0, faceDetected = false))
        assertNull(sm.onMetrics(metrics(elapsedMs = 100)))
        val lostAgain = sm.onMetrics(metrics(elapsedMs = 150, faceDetected = false))

        assertEquals(GestureType.FACE_LOST, firstLost?.type)
        assertNull(lostAgain)
    }

    @Test
    fun `large roll blocks head turn even when yaw exceeds threshold`() {
        val sm = GestureStateMachine(profile, maxHeadRollDeg = 12f)

        assertNull(sm.onMetrics(metrics(elapsedMs = 0, yawDeg = -25f, rollDeg = 20f)))
        assertNull(sm.onMetrics(metrics(elapsedMs = 310, yawDeg = -25f, rollDeg = 20f)))
    }

    @Test
    fun `confidence heuristic stays within 0 and 1`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, earLeft = 0.0f, earRight = 0.0f))
        sm.onMetrics(metrics(elapsedMs = 310, earLeft = 0.0f, earRight = 0.0f))
        val event = sm.onMetrics(metrics(elapsedMs = 320))

        assertTrue(event != null && event.confidence in 0f..1f)
    }
}
