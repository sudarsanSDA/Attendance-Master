package com.sda.attendance.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sda.attendance.data.model.Student
import com.sda.attendance.ui.viewmodel.AttendanceViewModel

@Composable
fun SettingsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
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

    var projectIdInput by remember { mutableStateOf(viewModel.syncManager.projectId) }
    var autoSyncEnabled by remember { mutableStateOf(viewModel.syncManager.isAutoSyncEnabled) }

    // Dialog for adding or editing a student
    if (showEditDialog) {
        StudentEditDialog(
            student = editingStudent,
            onDismiss = {
                showEditDialog = false
                editingStudent = null
            },
            onSave = { savedStudent ->
                viewModel.saveStudent(savedStudent)
                showEditDialog = false
                editingStudent = null
                Toast.makeText(context, "Student saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Default 20 Students?") },
            text = { Text("This will restore the original 20 PGECET/GATE student list and initial names. Past attendance sessions will remain intact.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetStudentsToDefault()
                        showResetDialog = false
                        Toast.makeText(context, "Reset to 20 students completed", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Settings & Management",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Edit student names, format initials & configure Firebase",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Name Formatting
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Name Formatting Shortcuts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quickly switch between initial format (e.g. 'A. CARTER', 'E. DAVIS') and full official names.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.convertAllToInitialNames()
                                Toast.makeText(context, "Converted to Initial Names!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Initial Names", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.convertAllToFullNames()
                                Toast.makeText(context, "Restored Full Names!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Full Names", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Firebase Backend Sync
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Firebase Cloud Backend", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (syncState.status == com.sda.attendance.data.remote.SyncStatus.SUCCESS) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (syncState.status == com.sda.attendance.data.remote.SyncStatus.SUCCESS) "SYNCED" else "OFFLINE READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (syncState.status == com.sda.attendance.data.remote.SyncStatus.SUCCESS) Color(0xFF15803D) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Attendance records are saved locally with automatic cloud backup to Firebase Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = projectIdInput,
                        onValueChange = {
                            projectIdInput = it
                            viewModel.syncManager.projectId = it
                        },
                        label = { Text("Firebase Project ID") },
                        placeholder = { Text("attendance-pgecet-gate") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Sync on Save", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = {
                                autoSyncEnabled = it
                                viewModel.syncManager.isAutoSyncEnabled = it
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.manualSyncToFirebase() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync All Sessions Now", fontWeight = FontWeight.Bold)
                    }

                    if (syncState.message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Status: ${syncState.message}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 3: Data Export & Backup
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Export & Backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Export attendance records to CSV for Excel / Sheets or copy full JSON backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val csv = viewModel.repository.exportToCsv()
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                    putExtra(Intent.EXTRA_SUBJECT, "Attendance_Export.csv")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share CSV Export"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share CSV", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val json = viewModel.repository.exportAllSessionsJson()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Attendance JSON", json)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied JSON backup to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy JSON", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 4: Student Roster Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage Students (${students.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { showBulkImportDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Paste List", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            editingStudent = null
                            showEditDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of all students with edit and delete controls
        items(students, key = { it.rollNumber }) { student ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = student.rollNumber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (student.isGate) Color(0xFF4F46E5) else Color(0xFF0284C7)
                            ) {
                                Text(
                                    text = if (student.isGate) "GATE" else "NON-GATE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = student.displayName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        if (student.fullName != student.displayName) {
                            Text(
                                text = student.fullName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            editingStudent = student
                            showEditDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Student", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = { viewModel.deleteStudent(student.rollNumber) }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Student", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StudentEditDialog(
    student: Student?,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    var rollNumber by remember { mutableStateOf(student?.rollNumber ?: "") }
    var fullName by remember { mutableStateOf(student?.fullName ?: "") }
    var displayName by remember { mutableStateOf(student?.displayName ?: "") }
    var isGate by remember { mutableStateOf(student?.isGate ?: false) }
    var phone by remember { mutableStateOf(student?.phoneNumber ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (student == null) "Add Student" else "Edit Student") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = rollNumber,
                    onValueChange = { rollNumber = it },
                    label = { Text("Roll Number") },
                    singleLine = true,
                    enabled = student == null // roll number is ID
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (displayName.isBlank() || displayName == student?.fullName) {
                            displayName = it
                        }
                    },
                    label = { Text("Full Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name (Initials)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GATE Student", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = isGate,
                        onCheckedChange = { isGate = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rollNumber.isNotBlank() && fullName.isNotBlank()) {
                        onSave(
                            Student(
                                rollNumber = rollNumber.trim(),
                                fullName = fullName.trim(),
                                displayName = displayName.ifBlank { fullName }.trim(),
                                isGate = isGate,
                                phoneNumber = phone.trim(),
                                gender = student?.gender ?: "",
                                category = student?.category ?: ""
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
