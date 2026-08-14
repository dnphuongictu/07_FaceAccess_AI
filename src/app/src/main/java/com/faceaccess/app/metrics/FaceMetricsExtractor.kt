package com.faceaccess.app.metrics

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Tinh EAR/MAR/head pose tu landmark khuon mat MediaPipe Face Landmarker
 * (topology 468/478 diem). Ham thuan (khong phu thuoc Android/MediaPipe SDK)
 * de co the kiem thu bang JUnit JVM thuan tuy, xem
 * app/src/test/java/com/faceaccess/app/metrics/FaceMetricsExtractorTest.kt.
 *
 * CANH BAO: yawDeg/pitchDeg/rollDeg suy tu facial transformation matrix cua
 * MediaPipe, quy uoc truc va dau (+/-) CHUA duoc doi chieu voi thiet bi that.
 * Khong duoc coi day la cung dinh nghia voi truong "yaw" minh hoa trong
 * data/inherited_safedrive — xem data/schema/README.md.
 */
object FaceMetricsExtractor {

    // Chi so landmark theo topology FaceMesh 468/478 diem cua MediaPipe.
    private val LEFT_EYE = intArrayOf(362, 385, 387, 263, 373, 380)
    private val RIGHT_EYE = intArrayOf(33, 160, 158, 133, 153, 144)
    private const val MOUTH_TOP = 13
    private const val MOUTH_BOTTOM = 14
    private const val MOUTH_LEFT = 61
    private const val MOUTH_RIGHT = 291

    private const val MIN_LANDMARK_COUNT = 468

    fun extract(
        landmarks: List<Landmark>,
        /**
         * Ma tran 4x4 flat COLUMN-MAJOR nhu MediaPipe
         * FaceLandmarkerResult.facialTransformationMatrixes() tra ve
         * (m[col*4+row]). Ham nay tu chuyen sang row-major truoc khi giai ma.
         */
        transformMatrixColumnMajor: FloatArray?,
        elapsedMs: Long,
    ): FaceMetrics {
        if (landmarks.size < MIN_LANDMARK_COUNT) {
            return FaceMetrics.noFace(elapsedMs)
        }
        val earLeft = eyeAspectRatio(landmarks, LEFT_EYE)
        val earRight = eyeAspectRatio(landmarks, RIGHT_EYE)
        val mar = mouthAspectRatio(landmarks)
        val euler = transformMatrixColumnMajor?.let { eulerAnglesDegFromMatrix(transpose4x4(it)) }
        return FaceMetrics(
            elapsedMs = elapsedMs,
            faceDetected = true,
            earLeft = earLeft,
            earRight = earRight,
            mar = mar,
            yawDeg = euler?.first ?: 0f,
            pitchDeg = euler?.second ?: 0f,
            rollDeg = euler?.third ?: 0f,
        )
    }

    private fun eyeAspectRatio(landmarks: List<Landmark>, idx: IntArray): Float {
        val p1 = landmarks[idx[0]]
        val p2 = landmarks[idx[1]]
        val p3 = landmarks[idx[2]]
        val p4 = landmarks[idx[3]]
        val p5 = landmarks[idx[4]]
        val p6 = landmarks[idx[5]]
        val vertical1 = distance(p2, p6)
        val vertical2 = distance(p3, p5)
        val horizontal = distance(p1, p4)
        return if (horizontal <= 1e-6f) 0f else (vertical1 + vertical2) / (2f * horizontal)
    }

    private fun mouthAspectRatio(landmarks: List<Landmark>): Float {
        val vertical = distance(landmarks[MOUTH_TOP], landmarks[MOUTH_BOTTOM])
        val horizontal = distance(landmarks[MOUTH_LEFT], landmarks[MOUTH_RIGHT])
        return if (horizontal <= 1e-6f) 0f else vertical / horizontal
    }

    private fun distance(a: Landmark, b: Landmark): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    /** Chuyen ma tran 4x4 flat tu column-major (m[col*4+row]) sang row-major (m[row*4+col]). */
    fun transpose4x4(m: FloatArray): FloatArray {
        require(m.size == 16) { "Can ma tran 4x4 (16 phan tu)" }
        val out = FloatArray(16)
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                out[row * 4 + col] = m[col * 4 + row]
            }
        }
        return out
    }

    /**
     * Giai ma goc Euler yaw(Y)/pitch(X)/roll(Z) tu ma tran xoay 4x4 row-major
     * (m[row*4+col]), quy uoc R = Rz(roll) * Ry(yaw) * Rx(pitch). Tra ve do.
     */
    fun eulerAnglesDegFromMatrix(m: FloatArray): Triple<Float, Float, Float> {
        require(m.size == 16) { "Can ma tran 4x4 (16 phan tu)" }
        val r00 = m[0]; val r01 = m[1]; val r02 = m[2]
        val r10 = m[4]; val r11 = m[5]; val r12 = m[6]
        val r20 = m[8]; val r21 = m[9]; val r22 = m[10]

        val pitchRad: Double
        val yawRad: Double
        val rollRad: Double

        val sinYaw = (-r20).toDouble().coerceIn(-1.0, 1.0)
        yawRad = asin(sinYaw)
        val cosYaw = cos(yawRad)
        if (cosYaw > 1e-6) {
            pitchRad = atan2(r21.toDouble(), r22.toDouble())
            rollRad = atan2(r10.toDouble(), r00.toDouble())
        } else {
            // Gimbal lock: yaw ~= +-90 do, gan roll = 0.
            pitchRad = atan2((-r12).toDouble(), r11.toDouble())
            rollRad = 0.0
        }

        val toDeg = 180.0 / Math.PI
        return Triple(
            (yawRad * toDeg).toFloat(),
            (pitchRad * toDeg).toFloat(),
            (rollRad * toDeg).toFloat(),
        )
    }
}
