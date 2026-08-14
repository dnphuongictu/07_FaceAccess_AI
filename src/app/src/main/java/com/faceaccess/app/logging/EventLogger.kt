package com.faceaccess.app.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * Ghi moi GestureEvent da qua cong chong kich hoat nham (khong ghi tung
 * frame thuong) ra JSON Lines trong bo nho rieng cua app
 * (filesDir/events/session_<id>.jsonl), dung dinh dang
 * data/schema/gesture_event.schema.json. Khong bao gio ghi anh.
 */
class EventLogger(context: Context, sessionId: String) {
    private val executor = Executors.newSingleThreadExecutor()
    private val file: File = File(
        File(context.applicationContext.filesDir, "events").apply { mkdirs() },
        "session_$sessionId.jsonl",
    )

    fun log(record: GestureEventRecord) {
        executor.execute {
            try {
                FileOutputStream(file, true).bufferedWriter().use { writer ->
                    writer.append(record.toJson().toString())
                    writer.append('\n')
                }
            } catch (e: Exception) {
                Log.e(TAG, "Khong ghi duoc event log", e)
            }
        }
    }

    fun eventsFilePath(): String = file.absolutePath

    fun close() {
        executor.shutdown()
    }

    companion object {
        private const val TAG = "EventLogger"
    }
}
