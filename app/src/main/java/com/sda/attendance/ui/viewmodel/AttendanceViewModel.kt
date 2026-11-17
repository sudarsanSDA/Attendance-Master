package com.sda.attendance.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sda.attendance.data.local.AttendanceRepository
import com.sda.attendance.data.model.AttendanceRecord
import com.sda.attendance.data.model.AttendanceSession
import com.sda.attendance.data.model.AttendanceStatus
import com.sda.attendance.data.model.Student
import com.sda.attendance.data.remote.FirebaseSyncManager
import com.sda.attendance.data.remote.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FilterCategory {
    ALL,
    GATE,
    NON_GATE,
    PRESENT,
    ABSENT
}

enum class ReportFormatType {
    FULL_DETAILED,
    ABSENTEES_ONLY,
    ROLL_NUMBERS_ONLY
}

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AttendanceRepository(application)
    val syncManager = FirebaseSyncManager(application, repository)

    val students: StateFlow<List<Student>> = repository.students
    val sessions: StateFlow<List<AttendanceSession>> = repository.sessions
    val syncState: StateFlow<SyncState> = syncManager.syncState

    // Current Session State
    val editingSessionId = MutableStateFlow<String?>(null)
    val sessionName = MutableStateFlow("Session 1")
    val sessionDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val sessionTime = MutableStateFlow(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()))
    val sessionNotes = MutableStateFlow("")
    val currentAttendance = MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())
    val lateMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    // UI Filters & Search
    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(FilterCategory.ALL)

    // Message Toast / Alert event
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadInitialData()
            initNewSession()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    /**
     * Initializes a fresh attendance session for the day.
     * Computes next session index for today (e.g. Session 1, Session 2, etc.)
     * Defaults all students to PRESENT for speed, or user can toggle them with a tap!
     */
    fun initNewSession() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todaysSessions = sessions.value.filter { it.date == todayStr }
        val nextIndex = todaysSessions.size + 1

        editingSessionId.value = null
        sessionName.value = "Session $nextIndex"
        sessionDate.value = todayStr
        sessionTime.value = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        sessionNotes.value = ""

        // Default all active students to PRESENT (faster to mark absentees or one-tap invert/clear)
        val initialMap = students.value.associate { it.rollNumber to AttendanceStatus.PRESENT }
        currentAttendance.value = initialMap
        lateMap.value = emptyMap()
        searchQuery.value = ""
        selectedFilter.value = FilterCategory.ALL
    }

    /**
     * Loads a past session for editing (e.g. adding late comers!)
     */
    fun loadSessionForEditing(session: AttendanceSession) {
        editingSessionId.value = session.id
        sessionName.value = session.sessionName
        sessionDate.value = session.date
        sessionTime.value = session.time
        sessionNotes.value = session.notes

        val statusMap = mutableMapOf<String, AttendanceStatus>()
        val lateRecords = mutableMapOf<String, Boolean>()

        students.value.forEach { s ->
            val rec = session.records[s.rollNumber]
            statusMap[s.rollNumber] = rec?.status ?: AttendanceStatus.ABSENT
            lateRecords[s.rollNumber] = rec?.isLate ?: false
        }

        currentAttendance.value = statusMap
        lateMap.value = lateRecords
        searchQuery.value = ""
        selectedFilter.value = FilterCategory.ALL
        _userMessage.value = "Editing ${session.sessionName} (${session.date})"
    }

    fun toggleAttendance(rollNumber: String) {
        val currentStatus = currentAttendance.value[rollNumber] ?: AttendanceStatus.ABSENT
        val newStatus = if (currentStatus == AttendanceStatus.PRESENT) {
            AttendanceStatus.ABSENT
        } else {
            AttendanceStatus.PRESENT
        }
        currentAttendance.value = currentAttendance.value + (rollNumber to newStatus)

        // If editing a past session and changing from ABSENT to PRESENT, mark as late comer!
        if (editingSessionId.value != null && newStatus == AttendanceStatus.PRESENT) {
            lateMap.value = lateMap.value + (rollNumber to true)
        }
    }

    fun toggleLate(rollNumber: String) {
        val current = lateMap.value[rollNumber] ?: false
        lateMap.value = lateMap.value + (rollNumber to !current)
        // Ensure student is also PRESENT if marked late
        if (!current) {
            currentAttendance.value = currentAttendance.value + (rollNumber to AttendanceStatus.PRESENT)
        }
    }

    fun markAllPresent() {
        val updated = students.value.associate { it.rollNumber to AttendanceStatus.PRESENT }
        currentAttendance.value = updated
    }

    fun markAllAbsent() {
        val updated = students.value.associate { it.rollNumber to AttendanceStatus.ABSENT }
        currentAttendance.value = updated
    }

    fun invertAttendance() {
        val current = currentAttendance.value
        val updated = students.value.associate { s ->
            val status = current[s.rollNumber] ?: AttendanceStatus.ABSENT
            s.rollNumber to if (status == AttendanceStatus.PRESENT) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
        }
        currentAttendance.value = updated
    }

    fun saveCurrentAttendance(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val recordMap = mutableMapOf<String, AttendanceRecord>()
            students.value.forEach { s ->
                val status = currentAttendance.value[s.rollNumber] ?: AttendanceStatus.ABSENT
                val isLate = lateMap.value[s.rollNumber] ?: false
                recordMap[s.rollNumber] = AttendanceRecord(
                    rollNumber = s.rollNumber,
                    status = status,
                    isLate = isLate,
                    markedAt = System.currentTimeMillis()
                )
            }

            val isEditMode = editingSessionId.value != null
            val sessionToSave = AttendanceSession(
                id = editingSessionId.value ?: java.util.UUID.randomUUID().toString(),
                date = sessionDate.value,
                time = sessionTime.value,
                sessionName = sessionName.value.ifBlank { "Session" },
                records = recordMap,
                notes = sessionNotes.value,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )

            repository.saveSession(sessionToSave)

            // Auto-sync with Firebase if enabled
            if (syncManager.isAutoSyncEnabled) {
                launch {
                    syncManager.syncSession(sessionToSave)
                }
            }

            val msg = if (isEditMode) {
                "✅ Session '${sessionToSave.sessionName}' updated successfully (Late comers added)!"
            } else {
                "✅ Attendance '${sessionToSave.sessionName}' saved successfully!"
            }
            _userMessage.value = msg

            initNewSession()
            onComplete(true)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            _userMessage.value = "Session deleted from logs"
        }
    }

    // -------------------------------------------------------------
    // Student Roster Operations
    // -------------------------------------------------------------
    fun saveStudent(student: Student) {
        viewModelScope.launch {
            repository.saveStudent(student)
            _userMessage.value = "Student ${student.rollNumber} saved"
        }
    }

    fun deleteStudent(rollNumber: String) {
        viewModelScope.launch {
            repository.deleteStudent(rollNumber)
            _userMessage.value = "Student $rollNumber deleted"
        }
    }

    fun convertAllToInitialNames() {
        viewModelScope.launch {
            repository.convertAllToInitialNames()
            _userMessage.value = "All names converted to initial format (e.g. A. CARTER)"
        }
    }

    fun convertAllToFullNames() {
        viewModelScope.launch {
            repository.convertAllToFullNames()
            _userMessage.value = "All names converted to full names"
        }
    }

    fun resetStudentsToDefault() {
        viewModelScope.launch {
            repository.resetStudentsToDefault()
            initNewSession()
            _userMessage.value = "Loaded demo students"
        }
    }

    fun bulkImportStudents(rawText: String, replaceExisting: Boolean = false, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val parsed = com.sda.attendance.data.model.RosterParser.parse(rawText)
            if (parsed.isNotEmpty()) {
                repository.bulkImportStudents(parsed, replaceExisting)
                initNewSession()
                _userMessage.value = "Successfully imported ${parsed.size} students!"
                onComplete(parsed.size)
            } else {
                _userMessage.value = "Could not parse students. Check format."
                onComplete(0)
            }
        }
    }

    fun clearAllStudents() {
        viewModelScope.launch {
            repository.clearAllStudents()
            initNewSession()
            _userMessage.value = "Cleared all students"
        }
    }

    fun loadSampleStudents() {
        viewModelScope.launch {
            repository.loadSampleStudents()
            initNewSession()
            _userMessage.value = "Loaded sample demo students"
        }
    }

    fun manualSyncToFirebase() {
        viewModelScope.launch {
            val count = syncManager.syncAllUnsyncedSessions()
            _userMessage.value = if (count > 0) {
                "Synced $count session(s) to Firebase!"
            } else {
                "All sessions are up to date with Firebase."
            }
        }
    }
}
