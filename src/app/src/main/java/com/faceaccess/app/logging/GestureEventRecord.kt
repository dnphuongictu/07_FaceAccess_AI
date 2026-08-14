package com.faceaccess.app.logging

import com.faceaccess.app.action.ActionResult
import com.faceaccess.app.gesture.GestureType
import org.json.JSONObject
import java.util.UUID

/**
 * Mot dong tuong ung data/schema/gesture_event.schema.json. Khong bao gio
 * chua anh hay dac trung sinh trac co the dinh danh khuon mat.
 */
data class GestureEventRecord(
    val sessionId: String,
    val participantCode: String,
    val deviceModel: String,
    val appVersion: String,
    val elapsedMs: Long,
    val faceDetected: Boolean,
    val earLeft: Float?,
    val earRight: Float?,
    val mar: Float?,
    val yawDeg: Float?,
    val pitchDeg: Float?,
    val rollDeg: Float?,
    val gestureType: GestureType,
    val gestureConfidence: Float?,
    val scanIndex: Int?,
    val actionMapped: String,
    val actionTarget: String?,
    val actionResult: ActionResult,
    val latencyMs: Long?,
    val fps: Float?,
    val eventId: String = UUID.randomUUID().toString(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("schema_version", "1.0")
        put("event_id", eventId)
        put("session_id", sessionId)
        put("participant_code", participantCode)
        put("device_model", deviceModel)
        put("app_version", appVersion)
        put("elapsed_ms", elapsedMs)
        put("face_detected", faceDetected)
        put("ear_left", earLeft?.toDouble() ?: JSONObject.NULL)
        put("ear_right", earRight?.toDouble() ?: JSONObject.NULL)
        put("mar", mar?.toDouble() ?: JSONObject.NULL)
        put("yaw_deg", yawDeg?.toDouble() ?: JSONObject.NULL)
        put("pitch_deg", pitchDeg?.toDouble() ?: JSONObject.NULL)
        put("roll_deg", rollDeg?.toDouble() ?: JSONObject.NULL)
        put("gesture_type", gestureType.schemaValue)
        put("gesture_confidence", gestureConfidence?.toDouble() ?: JSONObject.NULL)
        put("scan_index", scanIndex ?: JSONObject.NULL)
        put("action_mapped", actionMapped)
        put("action_target", actionTarget ?: JSONObject.NULL)
        put("action_result", actionResult.schemaValue)
        put("latency_ms", latencyMs?.toDouble() ?: JSONObject.NULL)
        put("fps", fps?.toDouble() ?: JSONObject.NULL)
        put("data_source", "device_live")
    }
}
