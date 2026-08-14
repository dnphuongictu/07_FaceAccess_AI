package com.faceaccess.app.gesture

/** Phai khop data/schema/calibration_profile.schema.json (tru cac truong danh tinh/thoi gian). */
data class CalibrationProfile(
    val earOpenBaseline: Float,
    val earClosedThreshold: Float,
    val yawLeftThresholdDeg: Float,
    val yawRightThresholdDeg: Float,
    val holdDurationMs: Long,
    val cooldownMs: Long,
    val emergencyHoldDurationMs: Long,
) {
    companion object {
        /** Nguong mac dinh khi chua hieu chinh; man hinh hieu chinh se ghi de. */
        val DEFAULT = CalibrationProfile(
            earOpenBaseline = 0.30f,
            earClosedThreshold = 0.18f,
            yawLeftThresholdDeg = 20f,
            yawRightThresholdDeg = 20f,
            holdDurationMs = 400L,
            cooldownMs = 600L,
            emergencyHoldDurationMs = 3000L,
        )
    }
}
