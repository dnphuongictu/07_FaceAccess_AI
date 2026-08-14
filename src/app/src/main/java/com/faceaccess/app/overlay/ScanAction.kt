package com.faceaccess.app.overlay

/** Mot muc co the duoc highlight/kich hoat trong danh sach quet cua overlay. */
sealed class ScanAction(val id: String, val label: String) {
    data object Back : ScanAction("back", "Quay lại")
    data object Home : ScanAction("home", "Màn hình chính")
    data object MediaPlayPause : ScanAction("media_play_pause", "Phát / Tạm dừng")
    data object EmergencyStop : ScanAction("emergency_stop", "Dừng khẩn cấp")
    data class OpenApp(val packageName: String, val appLabel: String) :
        ScanAction("open_app:$packageName", appLabel)

    /** Phai khop enum "action_mapped" trong data/schema/gesture_event.schema.json. */
    val actionMappedValue: String
        get() = when (this) {
            Back -> "back"
            Home -> "home"
            MediaPlayPause -> "media_play_pause"
            EmergencyStop -> "emergency_stop"
            is OpenApp -> "open_app"
        }
}
