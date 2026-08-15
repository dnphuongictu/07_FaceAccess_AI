package com.faceaccess.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.faceaccess.app.BuildConfig
import com.faceaccess.app.MainActivity
import com.faceaccess.app.R
import com.faceaccess.app.action.ActionDispatcher
import com.faceaccess.app.action.ActionResult
import com.faceaccess.app.camera.CameraSourceManager
import com.faceaccess.app.camera.FaceLandmarkerHelper
import com.faceaccess.app.data.CalibrationStore
import com.faceaccess.app.data.PinnedAppsStore
import com.faceaccess.app.feedback.AudioFeedback
import com.faceaccess.app.gesture.GestureEvent
import com.faceaccess.app.gesture.GestureStateMachine
import com.faceaccess.app.gesture.GestureType
import com.faceaccess.app.logging.EventLogger
import com.faceaccess.app.logging.GestureEventRecord
import com.faceaccess.app.metrics.FaceMetrics
import com.faceaccess.app.ui.theme.FaceAccessTheme
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Foreground Service la trung tam cua pipeline: chay CameraX + MediaPipe Face
 * Landmarker lien tuc, hien bang dieu khien noi (overlay) tren moi app khac,
 * chuyen GestureEvent thanh thao tac he thong qua ActionDispatcher, va ghi
 * log JSON Lines. Tu lam SavedStateRegistryOwner/ViewModelStoreOwner de host
 * ComposeView ngoai Activity (pattern chuan cho Compose trong Service).
 */
class FaceAccessOverlayService :
    LifecycleService(),
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null

    private var overlayUiState by mutableStateOf(OverlayUiState())

    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private lateinit var cameraSourceManager: CameraSourceManager
    private lateinit var gestureStateMachine: GestureStateMachine
    private lateinit var actionDispatcher: ActionDispatcher
    private lateinit var eventLogger: EventLogger
    private lateinit var audioFeedback: AudioFeedback
    private val scanController = ScanController()

    private val sessionId = UUID.randomUUID().toString()

    // Placeholder cho toi khi co man hinh nhap ma nguoi tham gia theo protocol
    // dong thuan; "local" chi dung cho tu kiem thu ky thuat.
    private val participantCode = "local"

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()

        eventLogger = EventLogger(this, sessionId)
        audioFeedback = AudioFeedback(this)
        gestureStateMachine = GestureStateMachine()
        actionDispatcher = ActionDispatcher(this, onEmergencyStop = { stopSelfService() })
        faceLandmarkerHelper = FaceLandmarkerHelper(this, faceLandmarkerListener)
        cameraSourceManager = CameraSourceManager(this, faceLandmarkerHelper)

        addOverlayView()

        lifecycleScope.launch {
            val calibrationStore = CalibrationStore(this@FaceAccessOverlayService)
            if (!calibrationStore.isCalibratedFlow.first()) {
                Log.e(TAG, "Tu choi khoi dong: chua co profile hieu chinh hop le")
                stopSelfService()
                return@launch
            }
            val initialProfile = calibrationStore.profileFlow.first()
            gestureStateMachine.updateProfile(initialProfile)
            faceLandmarkerHelper.setup()
            cameraSourceManager.start(this@FaceAccessOverlayService)

            calibrationStore.profileFlow.drop(1).collect { profile ->
                gestureStateMachine.updateProfile(profile)
            }
        }
        lifecycleScope.launch {
            val pinnedApps = PinnedAppsStore(this@FaceAccessOverlayService).pinnedAppsFlow.first()
            val actions = pinnedApps.map { ScanAction.OpenApp(it.packageName, it.label) } +
                listOf(ScanAction.Back, ScanAction.Home, ScanAction.MediaPlayPause, ScanAction.EmergencyStop)
            scanController.setActions(actions)
            refreshOverlayIndex()
            audioFeedback.announceReady(scanController.current?.label)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelfService()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        cameraSourceManager.stop()
        faceLandmarkerHelper.close()
        eventLogger.close()
        audioFeedback.close()
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        viewModelStore.clear()
        super.onDestroy()
    }

    private fun stopSelfService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---------------------------------------------------------------- Overlay window

    private fun addOverlayView() {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FaceAccessOverlayService)
            setViewTreeViewModelStoreOwner(this@FaceAccessOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FaceAccessOverlayService)
            setContent {
                FaceAccessTheme {
                    OverlayContent(
                        state = overlayUiState,
                        onStopClick = { handleEmergencyStopButton() },
                    )
                }
            }
        }
        overlayView = composeView

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 160
        }
        windowManager.addView(composeView, params)
    }

    private fun handleEmergencyStopButton() {
        audioFeedback.announceEmergencyStop()
        val result = actionDispatcher.dispatch(ScanAction.EmergencyStop)
        Log.i(TAG, "Dung khan cap qua nut cham: $result")
    }

    private fun refreshOverlayIndex() {
        overlayUiState = overlayUiState.copy(
            currentLabel = scanController.current?.label ?: "-",
            scanIndex = scanController.currentIndex,
            scanSize = scanController.size,
        )
    }

    // ---------------------------------------------------------------- Gesture pipeline

    private val faceLandmarkerListener = object : FaceLandmarkerHelper.Listener {
        override fun onFaceMetrics(metrics: FaceMetrics) {
            overlayUiState = overlayUiState.copy(
                faceDetected = metrics.faceDetected,
                earLeft = metrics.earLeft,
                earRight = metrics.earRight,
                yawDeg = metrics.yawDeg,
                pitchDeg = metrics.pitchDeg,
            )
            val event = gestureStateMachine.onMetrics(metrics) ?: return
            handleGestureEvent(event)
        }

        override fun onError(message: String) {
            Log.e(TAG, "FaceLandmarker loi: $message")
        }
    }

    private fun handleGestureEvent(event: GestureEvent) {
        when (event.type) {
            GestureType.HEAD_TURN_LEFT -> {
                scanController.movePrevious()
                refreshOverlayIndex()
                audioFeedback.announceSelection(scanController.current?.label)
                logEvent(event, actionMapped = "move_previous", actionTarget = null, result = ActionResult.EXECUTED)
            }
            GestureType.HEAD_TURN_RIGHT -> {
                scanController.moveNext()
                refreshOverlayIndex()
                audioFeedback.announceSelection(scanController.current?.label)
                logEvent(event, actionMapped = "move_next", actionTarget = null, result = ActionResult.EXECUTED)
            }
            GestureType.EYE_CLOSE_HOLD -> {
                val action = scanController.current
                if (action == null) {
                    logEvent(event, actionMapped = "none", actionTarget = null, result = ActionResult.IGNORED)
                } else {
                    if (action == ScanAction.EmergencyStop) audioFeedback.announceEmergencyStop()
                    val result = actionDispatcher.dispatch(action)
                    if (action != ScanAction.EmergencyStop) audioFeedback.announceAction(action.label, result)
                    val target = (action as? ScanAction.OpenApp)?.packageName
                    logEvent(event, actionMapped = action.actionMappedValue, actionTarget = target, result = result)
                }
            }
            GestureType.EYE_CLOSE_LONG -> {
                audioFeedback.announceEmergencyStop()
                val result = actionDispatcher.dispatch(ScanAction.EmergencyStop)
                logEvent(event, actionMapped = "emergency_stop", actionTarget = null, result = result)
            }
            GestureType.FACE_LOST -> {
                audioFeedback.announceFaceLost()
                logEvent(event, actionMapped = "none", actionTarget = null, result = ActionResult.IGNORED)
            }
            GestureType.NONE -> Unit
        }
    }

    private fun logEvent(event: GestureEvent, actionMapped: String, actionTarget: String?, result: ActionResult) {
        val record = GestureEventRecord(
            sessionId = sessionId,
            participantCode = participantCode,
            deviceModel = Build.MODEL ?: "unknown",
            appVersion = BuildConfig.VERSION_NAME,
            elapsedMs = event.elapsedMs,
            faceDetected = event.metrics.faceDetected,
            earLeft = event.metrics.earLeft,
            earRight = event.metrics.earRight,
            mar = event.metrics.mar,
            yawDeg = event.metrics.yawDeg,
            pitchDeg = event.metrics.pitchDeg,
            rollDeg = event.metrics.rollDeg,
            gestureType = event.type,
            gestureConfidence = event.confidence,
            scanIndex = scanController.currentIndex,
            actionMapped = actionMapped,
            actionTarget = actionTarget,
            actionResult = result,
            latencyMs = event.metrics.frameTimestampUptimeMs?.let { frameTime ->
                (SystemClock.uptimeMillis() - frameTime).coerceAtLeast(0L)
            },
            fps = event.metrics.fps,
        )
        eventLogger.log(record)
    }

    // ---------------------------------------------------------------- Notification

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.notification_channel_desc) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, FaceAccessOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.notification_action_stop), stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        private const val TAG = "FaceAccessOverlaySvc"
        private const val CHANNEL_ID = "face_access_overlay"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.faceaccess.app.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, FaceAccessOverlayService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, FaceAccessOverlayService::class.java).setAction(ACTION_STOP))
        }
    }
}
