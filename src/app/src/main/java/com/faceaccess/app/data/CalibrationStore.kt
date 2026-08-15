package com.faceaccess.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.faceaccess.app.gesture.CalibrationProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.calibrationDataStore: androidx.datastore.core.DataStore<Preferences> by
    preferencesDataStore(name = "face_access_calibration")

/** Luu ho so hieu chinh cuc bo tren thiet bi (khong dong bo len server). */
class CalibrationStore(private val context: Context) {

    val isCalibratedFlow: Flow<Boolean> = context.calibrationDataStore.data.map { prefs ->
        prefs[KEY_CALIBRATION_COMPLETED] == true
    }

    val profileFlow: Flow<CalibrationProfile> = context.calibrationDataStore.data.map { prefs ->
        CalibrationProfile(
            earOpenBaseline = prefs[KEY_EAR_OPEN_BASELINE] ?: CalibrationProfile.DEFAULT.earOpenBaseline,
            earClosedThreshold = prefs[KEY_EAR_CLOSED_THRESHOLD] ?: CalibrationProfile.DEFAULT.earClosedThreshold,
            yawLeftThresholdDeg = prefs[KEY_YAW_LEFT_THRESHOLD] ?: CalibrationProfile.DEFAULT.yawLeftThresholdDeg,
            yawRightThresholdDeg = prefs[KEY_YAW_RIGHT_THRESHOLD] ?: CalibrationProfile.DEFAULT.yawRightThresholdDeg,
            holdDurationMs = prefs[KEY_HOLD_DURATION_MS] ?: CalibrationProfile.DEFAULT.holdDurationMs,
            cooldownMs = prefs[KEY_COOLDOWN_MS] ?: CalibrationProfile.DEFAULT.cooldownMs,
            emergencyHoldDurationMs = prefs[KEY_EMERGENCY_HOLD_MS] ?: CalibrationProfile.DEFAULT.emergencyHoldDurationMs,
        )
    }

    suspend fun save(profile: CalibrationProfile) {
        context.calibrationDataStore.edit { prefs ->
            prefs[KEY_EAR_OPEN_BASELINE] = profile.earOpenBaseline
            prefs[KEY_EAR_CLOSED_THRESHOLD] = profile.earClosedThreshold
            prefs[KEY_YAW_LEFT_THRESHOLD] = profile.yawLeftThresholdDeg
            prefs[KEY_YAW_RIGHT_THRESHOLD] = profile.yawRightThresholdDeg
            prefs[KEY_HOLD_DURATION_MS] = profile.holdDurationMs
            prefs[KEY_COOLDOWN_MS] = profile.cooldownMs
            prefs[KEY_EMERGENCY_HOLD_MS] = profile.emergencyHoldDurationMs
            // Ghi cuoi cung trong cung mot transaction: profile chi duoc coi
            // la hop le sau khi toan bo nguong da duoc luu thanh cong.
            prefs[KEY_CALIBRATION_COMPLETED] = true
        }
    }

    private companion object {
        val KEY_EAR_OPEN_BASELINE = floatPreferencesKey("ear_open_baseline")
        val KEY_EAR_CLOSED_THRESHOLD = floatPreferencesKey("ear_closed_threshold")
        val KEY_YAW_LEFT_THRESHOLD = floatPreferencesKey("yaw_left_threshold_deg")
        val KEY_YAW_RIGHT_THRESHOLD = floatPreferencesKey("yaw_right_threshold_deg")
        val KEY_HOLD_DURATION_MS = longPreferencesKey("hold_duration_ms")
        val KEY_COOLDOWN_MS = longPreferencesKey("cooldown_ms")
        val KEY_EMERGENCY_HOLD_MS = longPreferencesKey("emergency_hold_duration_ms")
        val KEY_CALIBRATION_COMPLETED = booleanPreferencesKey("calibration_completed")
    }
}
