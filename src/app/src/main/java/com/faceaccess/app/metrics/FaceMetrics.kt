package com.faceaccess.app.metrics

/**
 * Mot khung so lieu tinh tu landmark khuon mat cua mot frame camera.
 * Khong bao gio chua anh hay dac trung co the dung de dinh danh khuon mat,
 * chi cac dai luong hinh hoc vo huong dung cho nhan dien cu chi.
 */
data class FaceMetrics(
    val elapsedMs: Long,
    val faceDetected: Boolean,
    val earLeft: Float,
    val earRight: Float,
    val mar: Float,
    val yawDeg: Float,
    val pitchDeg: Float,
    val rollDeg: Float,
) {
    companion object {
        fun noFace(elapsedMs: Long) = FaceMetrics(
            elapsedMs = elapsedMs,
            faceDetected = false,
            earLeft = 0f,
            earRight = 0f,
            mar = 0f,
            yawDeg = 0f,
            pitchDeg = 0f,
            rollDeg = 0f,
        )
    }
}
