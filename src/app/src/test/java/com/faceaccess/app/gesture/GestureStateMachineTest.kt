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
        faceDetected: Boolean = true,
    ) = FaceMetrics(
        elapsedMs = elapsedMs,
        faceDetected = faceDetected,
        earLeft = earLeft,
        earRight = earRight,
        mar = 0f,
        yawDeg = yawDeg,
        pitchDeg = 0f,
        rollDeg = 0f,
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
    fun `sustained eye closure past hold duration fires EYE_CLOSE_HOLD once`() {
        val sm = GestureStateMachine(profile)

        assertNull(sm.onMetrics(metrics(elapsedMs = 0, earLeft = 0.05f, earRight = 0.05f)))
        val event = sm.onMetrics(metrics(elapsedMs = 310, earLeft = 0.05f, earRight = 0.05f))

        assertEquals(GestureType.EYE_CLOSE_HOLD, event?.type)
    }

    @Test
    fun `holding closed eyes without releasing does not refire HOLD`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, earLeft = 0.05f, earRight = 0.05f))
        val first = sm.onMetrics(metrics(elapsedMs = 310, earLeft = 0.05f, earRight = 0.05f))
        val stillHeld = sm.onMetrics(metrics(elapsedMs = 500, earLeft = 0.05f, earRight = 0.05f))

        assertEquals(GestureType.EYE_CLOSE_HOLD, first?.type)
        assertNull(stillHeld)
    }

    @Test
    fun `continuing to hold eyes closed past emergency threshold fires EYE_CLOSE_LONG`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, earLeft = 0.05f, earRight = 0.05f))
        val hold = sm.onMetrics(metrics(elapsedMs = 310, earLeft = 0.05f, earRight = 0.05f))
        val long = sm.onMetrics(metrics(elapsedMs = 1010, earLeft = 0.05f, earRight = 0.05f))

        assertEquals(GestureType.EYE_CLOSE_HOLD, hold?.type)
        assertEquals(GestureType.EYE_CLOSE_LONG, long?.type)
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
    fun `confidence heuristic stays within 0 and 1`() {
        val sm = GestureStateMachine(profile)

        sm.onMetrics(metrics(elapsedMs = 0, earLeft = 0.0f, earRight = 0.0f))
        val event = sm.onMetrics(metrics(elapsedMs = 310, earLeft = 0.0f, earRight = 0.0f))

        assertTrue(event != null && event.confidence in 0f..1f)
    }
}
