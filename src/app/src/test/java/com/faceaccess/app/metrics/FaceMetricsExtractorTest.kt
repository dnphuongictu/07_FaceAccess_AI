package com.faceaccess.app.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class FaceMetricsExtractorTest {

    private fun landmarksWith(overrides: Map<Int, Landmark>): List<Landmark> {
        val list = MutableList(478) { Landmark(0f, 0f, 0f) }
        overrides.forEach { (index, landmark) -> list[index] = landmark }
        return list
    }

    @Test
    fun `open eye landmarks produce EAR around 0_3`() {
        val landmarks = landmarksWith(
            mapOf(
                // Mat trai: goc ngoai/trong + 2 cap tren-duoi
                362 to Landmark(0f, 0f, 0f),
                263 to Landmark(1f, 0f, 0f),
                385 to Landmark(0.3f, 0.15f, 0f),
                380 to Landmark(0.3f, -0.15f, 0f),
                387 to Landmark(0.7f, 0.15f, 0f),
                373 to Landmark(0.7f, -0.15f, 0f),
            ),
        )
        val metrics = FaceMetricsExtractor.extract(landmarks, transformMatrixColumnMajor = null, elapsedMs = 0)

        assertTrue(metrics.faceDetected)
        assertEquals(0.3f, metrics.earLeft, 0.01f)
    }

    @Test
    fun `closed eye landmarks produce much smaller EAR than open eye`() {
        val closed = landmarksWith(
            mapOf(
                362 to Landmark(0f, 0f, 0f),
                263 to Landmark(1f, 0f, 0f),
                385 to Landmark(0.3f, 0.02f, 0f),
                380 to Landmark(0.3f, -0.02f, 0f),
                387 to Landmark(0.7f, 0.02f, 0f),
                373 to Landmark(0.7f, -0.02f, 0f),
            ),
        )
        val metrics = FaceMetricsExtractor.extract(closed, transformMatrixColumnMajor = null, elapsedMs = 0)

        assertTrue(metrics.earLeft < 0.1f)
    }

    @Test
    fun `mouth aspect ratio matches vertical over horizontal distance`() {
        val landmarks = landmarksWith(
            mapOf(
                13 to Landmark(0.5f, 0.2f, 0f),
                14 to Landmark(0.5f, -0.2f, 0f),
                61 to Landmark(0f, 0f, 0f),
                291 to Landmark(1f, 0f, 0f),
            ),
        )
        val metrics = FaceMetricsExtractor.extract(landmarks, transformMatrixColumnMajor = null, elapsedMs = 0)

        assertEquals(0.4f, metrics.mar, 0.01f)
    }

    @Test
    fun `fewer than 468 landmarks yields no face detected`() {
        val metrics = FaceMetricsExtractor.extract(
            landmarks = listOf(Landmark(0f, 0f, 0f)),
            transformMatrixColumnMajor = null,
            elapsedMs = 42,
        )

        assertFalse(metrics.faceDetected)
        assertEquals(42L, metrics.elapsedMs)
    }

    /** Xay dung ma tran xoay row-major dung quy uoc R = Rz(roll) * Ry(yaw) * Rx(pitch), goc theo do. */
    private fun rotationMatrixRowMajor(yawDeg: Float, pitchDeg: Float, rollDeg: Float): FloatArray {
        val y = Math.toRadians(yawDeg.toDouble())
        val p = Math.toRadians(pitchDeg.toDouble())
        val r = Math.toRadians(rollDeg.toDouble())

        val r00 = (cos(r) * cos(y)).toFloat()
        val r01 = (cos(r) * sin(y) * sin(p) - sin(r) * cos(p)).toFloat()
        val r02 = (cos(r) * sin(y) * cos(p) + sin(r) * sin(p)).toFloat()
        val r10 = (sin(r) * cos(y)).toFloat()
        val r11 = (sin(r) * sin(y) * sin(p) + cos(r) * cos(p)).toFloat()
        val r12 = (sin(r) * sin(y) * cos(p) - cos(r) * sin(p)).toFloat()
        val r20 = (-sin(y)).toFloat()
        val r21 = (cos(y) * sin(p)).toFloat()
        val r22 = (cos(y) * cos(p)).toFloat()

        return floatArrayOf(
            r00, r01, r02, 0f,
            r10, r11, r12, 0f,
            r20, r21, r22, 0f,
            0f, 0f, 0f, 1f,
        )
    }

    @Test
    fun `eulerAnglesDegFromMatrix recovers pure yaw rotation`() {
        val matrix = rotationMatrixRowMajor(yawDeg = 30f, pitchDeg = 0f, rollDeg = 0f)

        val (yaw, pitch, roll) = FaceMetricsExtractor.eulerAnglesDegFromMatrix(matrix)

        assertEquals(30f, yaw, 0.5f)
        assertEquals(0f, pitch, 0.5f)
        assertEquals(0f, roll, 0.5f)
    }

    @Test
    fun `eulerAnglesDegFromMatrix recovers combined yaw pitch roll`() {
        val matrix = rotationMatrixRowMajor(yawDeg = -20f, pitchDeg = 10f, rollDeg = 5f)

        val (yaw, pitch, roll) = FaceMetricsExtractor.eulerAnglesDegFromMatrix(matrix)

        assertEquals(-20f, yaw, 0.5f)
        assertEquals(10f, pitch, 0.5f)
        assertEquals(5f, roll, 0.5f)
    }

    @Test
    fun `transpose4x4 converts column-major MediaPipe matrix to row-major before decoding`() {
        val rowMajor = rotationMatrixRowMajor(yawDeg = 25f, pitchDeg = 0f, rollDeg = 0f)
        // Chuyen sang column-major nhu MediaPipe tra ve (m[col*4+row]).
        val columnMajor = FloatArray(16)
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                columnMajor[col * 4 + row] = rowMajor[row * 4 + col]
            }
        }

        val landmarks = MutableList(478) { Landmark(0f, 0f, 0f) }.also { list ->
            list[362] = Landmark(0f, 0f, 0f)
            list[263] = Landmark(1f, 0f, 0f)
            list[385] = Landmark(0.3f, 0.15f, 0f)
            list[380] = Landmark(0.3f, -0.15f, 0f)
            list[387] = Landmark(0.7f, 0.15f, 0f)
            list[373] = Landmark(0.7f, -0.15f, 0f)
        }

        val metrics = FaceMetricsExtractor.extract(landmarks, columnMajor, elapsedMs = 0)

        assertEquals(25f, metrics.yawDeg, 0.5f)
    }
}
