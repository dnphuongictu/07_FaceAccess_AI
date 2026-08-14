package com.faceaccess.app.gesture

import com.faceaccess.app.metrics.FaceMetrics

/**
 * Mot cu chi da vuot qua cong chong kich hoat nham cua GestureStateMachine.
 * confidence hien la heuristic don gian (xem GestureStateMachine), khong
 * phai xac suat hieu chuan tu model.
 */
data class GestureEvent(
    val type: GestureType,
    val elapsedMs: Long,
    val metrics: FaceMetrics,
    val confidence: Float,
)
