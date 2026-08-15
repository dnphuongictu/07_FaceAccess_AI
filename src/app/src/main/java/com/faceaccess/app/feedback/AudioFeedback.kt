package com.faceaccess.app.feedback

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.faceaccess.app.action.ActionResult
import java.util.Locale

/**
 * Phan hoi khong can nhin man hinh: TTS doc huong dan, muc dang highlight
 * hoac ket qua hanh dong; rung ngan xac nhan cu chi da duoc nhan.
 */
class AudioFeedback(context: Context) : TextToSpeech.OnInitListener, AutoCloseable {

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    private val textToSpeech = TextToSpeech(context.applicationContext, this)
    private var ttsReady = false
    private var pendingText: String? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Khong khoi tao duoc Text-to-Speech, chi dung tone")
            return
        }
        textToSpeech.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        val vietnameseResult = textToSpeech.setLanguage(Locale("vi", "VN"))
        if (vietnameseResult == TextToSpeech.LANG_MISSING_DATA ||
            vietnameseResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            Log.w(TAG, "May khong co voice TTS tieng Viet; dung ngon ngu mac dinh")
            textToSpeech.language = Locale.getDefault()
        }
        ttsReady = true
        pendingText?.let { speak(it) }
        pendingText = null
    }

    fun announceReady(currentLabel: String?) {
        if (currentLabel == null) return
        speakOrQueue(
            "Face Access sẵn sàng. Hãy quay đầu sang phải để nghe mục tiếp theo, " +
                "hoặc sang trái để nghe mục trước. Khi nghe đúng mục, hãy nhắm mắt ngắn rồi mở để chọn. " +
                "Đang chọn $currentLabel",
        )
    }

    fun announceInstruction(instruction: String) {
        playTick()
        speakOrQueue(instruction)
    }

    fun announceSelection(label: String?) {
        playTick()
        if (label != null) {
            speakOrQueue(
                "Đang chọn $label. Hãy nhắm mắt rồi mở để xác nhận, hoặc quay đầu để đổi mục",
            )
        }
    }

    fun announceAction(label: String, result: ActionResult) {
        if (result == ActionResult.EXECUTED) {
            playConfirm()
            speakOrQueue("Đã chọn $label")
        } else {
            playWarning()
            speakOrQueue("Không thể thực hiện $label")
        }
    }

    fun announceFaceLost() {
        playWarning()
        speakOrQueue("Không thấy khuôn mặt. Đã tạm dừng điều khiển. Hãy nhìn thẳng vào camera")
    }

    fun announceEmergencyStop() {
        // Phat tone truoc khi stopSelf() huy TTS cua service.
        playWarning(durationMs = 400)
        speakOrQueue("Dừng Face Access")
    }

    private fun speakOrQueue(text: String) {
        if (ttsReady) speak(text) else pendingText = text
    }

    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun playTick() {
        vibrate(55)
    }

    private fun playConfirm() {
        vibrate(90)
    }

    private fun playWarning(durationMs: Int = 250) {
        vibrate(durationMs.coerceAtMost(300).toLong())
    }

    @Suppress("DEPRECATION")
    private fun vibrate(durationMs: Long) {
        val availableVibrator = vibrator ?: return
        if (!availableVibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            availableVibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            availableVibrator.vibrate(durationMs)
        }
    }

    override fun close() {
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    companion object {
        private const val TAG = "AudioFeedback"
        private const val UTTERANCE_ID = "face_access_feedback"
    }
}
