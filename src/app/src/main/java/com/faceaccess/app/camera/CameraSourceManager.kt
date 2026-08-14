package com.faceaccess.app.camera

import android.content.Context
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Gan CameraX toi mot LifecycleOwner do caller cung cap, day tung frame vao
 * FaceLandmarkerHelper tren luong nen rieng. Overlay service khong dung
 * Preview (khong hien thi hinh camera), nhung man hinh Hieu chinh co the
 * truyen previewSurfaceProvider de nguoi dung tu nhin thay camera.
 */
class CameraSourceManager(
    private val context: Context,
    private val faceLandmarkerHelper: FaceLandmarkerHelper,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysisExecutor: ExecutorService? = null

    fun start(lifecycleOwner: LifecycleOwner, previewSurfaceProvider: Preview.SurfaceProvider? = null) {
        val executor = Executors.newSingleThreadExecutor()
        analysisExecutor = executor

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            bind(provider, lifecycleOwner, executor, previewSurfaceProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bind(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        executor: ExecutorService,
        previewSurfaceProvider: Preview.SurfaceProvider?,
    ) {
        provider.unbindAll()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            faceLandmarkerHelper.detect(imageProxy, isFrontCamera = true)
        }

        val useCases = mutableListOf<UseCase>(imageAnalysis)
        if (previewSurfaceProvider != null) {
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(previewSurfaceProvider) }
            useCases.add(preview)
        }

        provider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases.toTypedArray())
    }

    fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        analysisExecutor?.shutdown()
        analysisExecutor = null
    }
}
