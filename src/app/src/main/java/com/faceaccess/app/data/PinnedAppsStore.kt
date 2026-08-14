package com.faceaccess.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class PinnedApp(val packageName: String, val label: String)

private val Context.pinnedAppsDataStore: androidx.datastore.core.DataStore<Preferences> by
    preferencesDataStore(name = "face_access_pinned_apps")

/** Danh sach ung dung nguoi dung ghim de mo bang cu chi, luu cuc bo. */
class PinnedAppsStore(private val context: Context) {

    val pinnedAppsFlow: Flow<List<PinnedApp>> = context.pinnedAppsDataStore.data.map { prefs ->
        parse(prefs[KEY_PINNED_APPS_JSON] ?: "[]")
    }

    suspend fun setPinnedApps(apps: List<PinnedApp>) {
        context.pinnedAppsDataStore.edit { prefs ->
            prefs[KEY_PINNED_APPS_JSON] = serialize(apps)
        }
    }

    suspend fun addPinnedApp(app: PinnedApp) {
        context.pinnedAppsDataStore.edit { prefs ->
            val current = parse(prefs[KEY_PINNED_APPS_JSON] ?: "[]").toMutableList()
            if (current.none { it.packageName == app.packageName }) current.add(app)
            prefs[KEY_PINNED_APPS_JSON] = serialize(current)
        }
    }

    suspend fun removePinnedApp(packageName: String) {
        context.pinnedAppsDataStore.edit { prefs ->
            val current = parse(prefs[KEY_PINNED_APPS_JSON] ?: "[]").filterNot { it.packageName == packageName }
            prefs[KEY_PINNED_APPS_JSON] = serialize(current)
        }
    }

    private fun parse(raw: String): List<PinnedApp> {
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            PinnedApp(obj.getString("package_name"), obj.getString("label"))
        }
    }

    private fun serialize(apps: List<PinnedApp>): String {
        val arr = JSONArray()
        apps.forEach { app ->
            arr.put(
                JSONObject().apply {
                    put("package_name", app.packageName)
                    put("label", app.label)
                },
            )
        }
        return arr.toString()
    }

    private companion object {
        val KEY_PINNED_APPS_JSON = stringPreferencesKey("pinned_apps_json")
    }
}
