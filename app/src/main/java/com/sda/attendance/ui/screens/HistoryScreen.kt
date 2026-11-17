package com.sda.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sda.attendance.data.model.AttendanceSession
import com.sda.attendance.ui.components.FormatSelectionDialog
import com.sda.attendance.ui.components.SessionSummaryCard
import com.sda.attendance.ui.viewmodel.AttendanceViewModel

@Composable
fun HistoryScreen(
    viewModel: AttendanceViewModel,
    onNavigateToTakeAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val students by viewModel.students.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSessionForFormat by remember { mutableStateOf<AttendanceSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<AttendanceSession?>(null) }

    // Filter sessions
    val filteredSessions = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) sessions else {
            sessions.filter {
                it.sessionName.contains(searchQuery, ignoreCase = true) ||
                        it.date.contains(searchQuery, ignoreCase = true) ||
                        it.time.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Group sessions by date
    val groupedSessions = remember(filteredSessions) {
        filteredSessions.groupBy { it.date }
    }

    if (selectedSessionForFormat != null) {
        FormatSelectionDialog(
            session = selectedSessionForFormat!!,
            students = students,
            onDismiss = { selectedSessionForFormat = null }
        )
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Attendance Record?") },
            text = { Text("Are you sure you want to delete '${sessionToDelete!!.sessionName}' from ${sessionToDelete!!.date}? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(sessionToDelete!!.id)
                        sessionToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.initNewSession()
                    onNavigateToTakeAttendance()
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Take Attendance", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Screen Header
                Text(
                    text = "Attendance Logs & History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All sessions recorded daily • One-tap copy & edit late comers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search logs by date or session...") },
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

            if (sessions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Attendance Logs Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Take your first attendance session to build daily logs and share reports with your teachers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.initNewSession()
                                    onNavigateToTakeAttendance()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Take First Attendance")
                            }
                        }
                    }
                }
            } else {
                // Group by date
                groupedSessions.forEach { (date, sessionList) ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅 $date",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${sessionList.size} session(s)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    items(sessionList, key = { it.id }) { session ->
                        SessionSummaryCard(
                            session = session,
                            students = students,
                            onEdit = {
                                viewModel.loadSessionForEditing(session)
                                onNavigateToTakeAttendance()
                            },
                            onDelete = { sessionToDelete = session },
                            onOpenFormatDialog = { selectedSessionForFormat = session }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}
