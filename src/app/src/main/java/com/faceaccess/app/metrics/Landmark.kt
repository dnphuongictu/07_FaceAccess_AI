package com.faceaccess.app.metrics

/**
 * Toa do landmark khuon mat, tach khoi kieu du lieu cua MediaPipe de
 * FaceMetricsExtractor kiem thu duoc bang JUnit thuan, khong can Robolectric.
 */
data class Landmark(val x: Float, val y: Float, val z: Float)
