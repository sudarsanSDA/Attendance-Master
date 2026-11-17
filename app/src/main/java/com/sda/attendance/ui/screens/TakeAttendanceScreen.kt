package com.sda.attendance.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sda.attendance.data.model.AttendanceRecord
import com.sda.attendance.data.model.AttendanceSession
import com.sda.attendance.data.model.AttendanceStatus
import com.sda.attendance.ui.components.FormatSelectionDialog
import com.sda.attendance.ui.components.StatsHeaderCard
import com.sda.attendance.ui.components.StudentAttendanceCard
import com.sda.attendance.ui.viewmodel.AttendanceViewModel
import com.sda.attendance.ui.viewmodel.FilterCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val attendanceMap by viewModel.currentAttendance.collectAsState()
    val lateMap by viewModel.lateMap.collectAsState()
    val editingSessionId by viewModel.editingSessionId.collectAsState()

    var sessionName by remember { mutableStateOf(viewModel.sessionName.value) }
    LaunchedEffect(viewModel.sessionName.collectAsState().value) {
        sessionName = viewModel.sessionName.value
    }

    val sessionDate by viewModel.sessionDate.collectAsState()
    val sessionTime by viewModel.sessionTime.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(FilterCategory.ALL) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showBulkImportDialog by remember { mutableStateOf(false) }

    if (showBulkImportDialog) {
        com.sda.attendance.ui.components.BulkImportDialog(
            onDismiss = { showBulkImportDialog = false },
            onImport = { text, replace ->
                viewModel.bulkImportStudents(text, replace) { count ->
                    Toast.makeText(context, "Imported $count students!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Computations for live stats
    val totalStudents = students.size
    val presentCount = students.count { attendanceMap[it.rollNumber] == AttendanceStatus.PRESENT }
    val absentCount = totalStudents - presentCount

    val gateStudents = students.filter { it.isGate }
    val gatePresent = gateStudents.count { attendanceMap[it.rollNumber] == AttendanceStatus.PRESENT }

    val nonGateStudents = students.filter { !it.isGate }
    val nonGatePresent = nonGateStudents.count { attendanceMap[it.rollNumber] == AttendanceStatus.PRESENT }

    // Filtered student list
    val filteredStudents = remember(students, attendanceMap, searchQuery, selectedFilter) {
        students.filter { student ->
            val matchesSearch = student.rollNumber.contains(searchQuery, ignoreCase = true) ||
                    student.fullName.contains(searchQuery, ignoreCase = true) ||
                    student.displayName.contains(searchQuery, ignoreCase = true)

            val status = attendanceMap[student.rollNumber] ?: AttendanceStatus.ABSENT
            val matchesFilter = when (selectedFilter) {
                FilterCategory.ALL -> true
                FilterCategory.GATE -> student.isGate
                FilterCategory.NON_GATE -> !student.isGate
                FilterCategory.PRESENT -> status == AttendanceStatus.PRESENT
                FilterCategory.ABSENT -> status == AttendanceStatus.ABSENT
            }

            matchesSearch && matchesFilter
        }
    }

    // Temporary session object for live copying before or after save
    val activeSessionPreview = remember(
        editingSessionId, sessionDate, sessionTime, sessionName, attendanceMap, lateMap, students
    ) {
        val recordMap = students.associate { s ->
            s.rollNumber to AttendanceRecord(
                rollNumber = s.rollNumber,
                status = attendanceMap[s.rollNumber] ?: AttendanceStatus.ABSENT,
                isLate = lateMap[s.rollNumber] ?: false
            )
        }
        AttendanceSession(
            id = editingSessionId ?: "temp",
            date = sessionDate,
            time = sessionTime,
            sessionName = sessionName.ifBlank { "Session" },
            records = recordMap
        )
    }

    if (showFormatDialog) {
        FormatSelectionDialog(
            session = activeSessionPreview,
            students = students,
            onDismiss = { showFormatDialog = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Bottom Action Bar: Save / Update Session and Quick Copy
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Copy Message Button (One Click)
                    OutlinedButton(
                        onClick = {
                            val report = activeSessionPreview.formatTeacherReport(students)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Attendance Report", report)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "📋 Copied teacher report to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(0.42f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontWeight = FontWeight.Bold)
                    }

                    // Share button
                    IconButton(
                        onClick = { showFormatDialog = true }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Primary Save/Update Button
                    Button(
                        onClick = {
                            viewModel.sessionName.value = sessionName
                            viewModel.saveCurrentAttendance {
                                Toast.makeText(context, "Attendance saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (editingSessionId != null) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(0.58f)
                    ) {
                        Icon(
                            imageVector = if (editingSessionId != null) Icons.Default.Edit else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (editingSessionId != null) "Update Session" else "Save Session",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Editing Mode Alert Banner
                if (editingSessionId != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFFB45309)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Editing Past Session (Late Comers)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Tap late comers to mark Present, then tap 'Update Session'.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                            TextButton(onClick = { viewModel.initNewSession() }) {
                                Text("Cancel", color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Session Details Card: Date, Time & Session Name Input
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📅 $sessionDate  •  ⏰ $sessionTime",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = sessionName,
                                onValueChange = {
                                    sessionName = it
                                    viewModel.sessionName.value = it
                                },
                                label = { Text("Session Name / Slot") },
                                placeholder = { Text("e.g. Session 1, Lab, Unit 2") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Reset / New session button
                        IconButton(
                            onClick = { viewModel.initNewSession() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "New Session", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Live Attendance Statistics Card
            item {
                StatsHeaderCard(
                    presentCount = presentCount,
                    absentCount = absentCount,
                    totalCount = totalStudents,
                    gatePresent = gatePresent,
                    gateTotal = gateStudents.size,
                    nonGatePresent = nonGatePresent,
                    nonGateTotal = nonGateStudents.size
                )
            }

            // Quick Batch Actions Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.markAllPresent() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All Present", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.markAllAbsent() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All Absent", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.invertAttendance() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invert", fontSize = 12.sp)
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by roll no or name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Filter Chips
            item {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == FilterCategory.ALL,
                        onClick = { selectedFilter = FilterCategory.ALL },
                        label = { Text("All ($totalStudents)") }
                    )
                    FilterChip(
                        selected = selectedFilter == FilterCategory.GATE,
                        onClick = { selectedFilter = FilterCategory.GATE },
                        label = { Text("🎓 GATE (${gateStudents.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == FilterCategory.NON_GATE,
                        onClick = { selectedFilter = FilterCategory.NON_GATE },
                        label = { Text("📚 NON-GATE (${nonGateStudents.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == FilterCategory.PRESENT,
                        onClick = { selectedFilter = FilterCategory.PRESENT },
                        label = { Text("✅ Present ($presentCount)") }
                    )
                    FilterChip(
                        selected = selectedFilter == FilterCategory.ABSENT,
                        onClick = { selectedFilter = FilterCategory.ABSENT },
                        label = { Text("❌ Absent ($absentCount)") }
                    )
                }
            }

            if (students.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Welcome to Attendance Master! 🎓",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No students in your roster yet. You can paste a student list from Excel/WhatsApp or load demo students.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { showBulkImportDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Paste List", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.loadSampleStudents() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Load Demo")
                                }
                            }
                        }
                    }
                }
            } else {
                // Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Students List (${filteredStudents.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap card to toggle status",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Student Cards
                items(filteredStudents, key = { it.rollNumber }) { student ->
                    val status = attendanceMap[student.rollNumber] ?: AttendanceStatus.ABSENT
                    val isLate = lateMap[student.rollNumber] ?: false

                    StudentAttendanceCard(
                        student = student,
                        status = status,
                        isLate = isLate,
                        onToggleStatus = { viewModel.toggleAttendance(student.rollNumber) },
                        onToggleLate = { viewModel.toggleLate(student.rollNumber) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
