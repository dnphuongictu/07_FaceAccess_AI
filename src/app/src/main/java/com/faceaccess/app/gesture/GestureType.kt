package com.faceaccess.app.gesture

/** Phai khop enum "gesture_type" trong data/schema/gesture_event.schema.json. */
enum class GestureType(val schemaValue: String) {
    NONE("none"),
    HEAD_TURN_LEFT("head_turn_left"),
    HEAD_TURN_RIGHT("head_turn_right"),
    EYE_CLOSE_HOLD("eye_close_hold"),
    EYE_CLOSE_LONG("eye_close_long"),
    FACE_LOST("face_lost"),
}
