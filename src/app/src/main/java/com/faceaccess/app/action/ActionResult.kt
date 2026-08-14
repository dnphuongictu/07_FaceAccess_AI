package com.faceaccess.app.action

/** Phai khop enum "action_result" trong data/schema/gesture_event.schema.json. */
enum class ActionResult(val schemaValue: String) {
    EXECUTED("executed"),
    BLOCKED_NO_ACCESSIBILITY_SERVICE("blocked_no_accessibility_service"),
    BLOCKED_COOLDOWN("blocked_cooldown"),
    BLOCKED_LOW_CONFIDENCE("blocked_low_confidence"),
    IGNORED("ignored"),
}
