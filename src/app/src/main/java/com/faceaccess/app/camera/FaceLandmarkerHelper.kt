package com.faceaccess.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.faceaccess.app.metrics.FaceMetrics
import com.faceaccess.app.metrics.FaceMetricsExtractor
import com.faceaccess.app.metrics.Landmark
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * Boc MediaPipe Face Landmarker (Tasks Vision) o che do LIVE_STREAM va quy
 * doi ket qua sang FaceMetrics cua rieng FaceAccess. Model duoc doc tu
 * assets/face_landmarker.task (dong goi offline, khong tai qua mang luc chay).
 */
class FaceLandmarkerHelper(
    private val context: Context,
    private val listener: Listener,
) {
    interface Listener {
        fun onFaceMetrics(metrics: FaceMetrics)
        fun onError(message: String)
    }

    private var faceLandmarker: FaceLandmarker? = null
    private var sessionStartUptimeMs: Long = SystemClock.uptimeMillis()

    fun setup() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_ASSET_PATH)
                .build()
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .setOutputFaceBlendshapes(false)
                .setOutputFacialTransformationMatrixes(true)
                .setResultListener(::onResult)
                .setErrorListener(::onMediapipeError)
                .build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            sessionStartUptimeMs = SystemClock.uptimeMillis()
        } catch (e: Exception) {
            listener.onError("Khong khoi tao duoc Face Landmarker: ${e.message}")
            Log.e(TAG, "setup() that bai", e)
        }
    }

    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    /** Goi tu ImageAnalysis.Analyzer; ham nay chiu trach nhiem dong imageProxy. */
    fun detect(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val landmarker = faceLandmarker
        if (landmarker == null) {
            imageProxy.close()
            return
        }
        val frameTime = SystemClock.uptimeMillis()
        val bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            }
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true,
        )
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        landmarker.detectAsync(mpImage, frameTime)
    }

    private fun onResult(result: FaceLandmarkerResult, input: MPImage) {
        val elapsedMs = result.timestampMs() - sessionStartUptimeMs
        val faces = result.faceLandmarks()
        if (faces.isEmpty()) {
            listener.onFaceMetrics(FaceMetrics.noFace(elapsedMs))
            return
        }
        val landmarks = faces[0].map { Landmark(it.x(), it.y(), it.z()) }
        val matrixColumnMajor = result.facialTransformationMatrixes().orElse(null)?.firstOrNull()
        val metrics = FaceMetricsExtractor.extract(landmarks, matrixColumnMajor, elapsedMs)
        listener.onFaceMetrics(metrics)
    }

    private fun onMediapipeError(error: RuntimeException) {
        listener.onError(error.message ?: "Loi khong xac dinh tu MediaPipe")
    }

    companion object {
        private const val TAG = "FaceLandmarkerHelper"
        private const val MODEL_ASSET_PATH = "face_landmarker.task"
    }
}
