package com.sda.attendance.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.Gson
import com.sda.attendance.data.local.AttendanceRepository
import com.sda.attendance.data.model.AttendanceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    OFFLINE_LOCAL_ONLY,
    ERROR
}

data class SyncState(
    val status: SyncStatus = SyncStatus.IDLE,
    val lastSyncTime: Long = 0L,
    val message: String = "Ready"
)

class FirebaseSyncManager(
    private val context: Context,
    private val repository: AttendanceRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("firebase_attendance_config", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    companion object {
        private const val KEY_PROJECT_ID = "firebase_project_id"
        private const val KEY_API_KEY = "firebase_api_key"
        private const val KEY_COLLECTION = "firebase_collection"
        private const val KEY_AUTO_SYNC = "firebase_auto_sync"
        private const val DEFAULT_COLLECTION = "attendance_sessions"
    }

    var projectId: String
        get() = prefs.getString(KEY_PROJECT_ID, "attendance-pgecet-gate") ?: "attendance-pgecet-gate"
        set(value) = prefs.edit().putString(KEY_PROJECT_ID, value.trim()).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var collectionName: String
        get() = prefs.getString(KEY_COLLECTION, DEFAULT_COLLECTION) ?: DEFAULT_COLLECTION
        set(value) = prefs.edit().putString(KEY_COLLECTION, value.trim()).apply()

    var isAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC, value).apply()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun syncSession(session: AttendanceSession): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            _syncState.value = SyncState(
                status = SyncStatus.OFFLINE_LOCAL_ONLY,
                message = "Saved locally (Offline). Will sync when online."
            )
            return@withContext false
        }

        _syncState.value = SyncState(status = SyncStatus.SYNCING, message = "Syncing with Firebase...")

        try {
            // Firestore REST API: PATCH or POST to document
            val targetUrl = if (apiKey.isNotBlank()) {
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collectionName/${session.id}?key=$apiKey"
            } else {
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collectionName/${session.id}"
            }

            // Convert session to Firestore Document fields payload
            val firestorePayload = mapOf(
                "fields" to mapOf(
                    "id" to mapOf("stringValue" to session.id),
                    "date" to mapOf("stringValue" to session.date),
                    "time" to mapOf("stringValue" to session.time),
                    "sessionName" to mapOf("stringValue" to session.sessionName),
                    "presentCount" to mapOf("integerValue" to session.presentCount.toString()),
                    "absentCount" to mapOf("integerValue" to session.absentCount.toString()),
                    "totalCount" to mapOf("integerValue" to session.totalCount.toString()),
                    "notes" to mapOf("stringValue" to session.notes),
                    "createdAt" to mapOf("integerValue" to session.createdAt.toString()),
                    "updatedAt" to mapOf("integerValue" to session.updatedAt.toString()),
                    "recordsJson" to mapOf("stringValue" to gson.toJson(session.records))
                )
            )

            val jsonString = gson.toJson(firestorePayload)
            val url = URL(targetUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonString)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                repository.markSessionSynced(session.id, true)
                _syncState.value = SyncState(
                    status = SyncStatus.SUCCESS,
                    lastSyncTime = System.currentTimeMillis(),
                    message = "Synced to Firebase Firestore successfully!"
                )
                true
            } else {
                // If the project doesn't have open security rules or requires auth, record local success
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                _syncState.value = SyncState(
                    status = SyncStatus.OFFLINE_LOCAL_ONLY,
                    message = "Saved locally. Firebase returned ($responseCode). Check Project ID/Rules."
                )
                false
            }
        } catch (e: Exception) {
            _syncState.value = SyncState(
                status = SyncStatus.OFFLINE_LOCAL_ONLY,
                message = "Saved locally. Firebase sync paused: ${e.localizedMessage ?: "Network issue"}"
            )
            false
        }
    }

    suspend fun syncAllUnsyncedSessions(): Int = withContext(Dispatchers.IO) {
        val sessions = repository.sessions.value
        var count = 0
        sessions.forEach { session ->
            if (!session.isSynced) {
                val ok = syncSession(session)
                if (ok) count++
            }
        }
        count
    }
}
