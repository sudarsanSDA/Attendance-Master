package com.sda.attendance.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sda.attendance.data.model.AttendanceSession
import com.sda.attendance.data.model.AttendanceStatus
import com.sda.attendance.data.model.Student

@Composable
fun SessionSummaryCard(
    session: AttendanceSession,
    students: List<Student>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenFormatDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val studentMap = remember(students) { students.associateBy { it.rollNumber } }
    val gateStudents = remember(students) { students.filter { it.isGate } }
    val nonGateStudents = remember(students) { students.filter { !it.isGate } }

    val gatePresent = remember(session, gateStudents) {
        gateStudents.count { session.records[it.rollNumber]?.status == AttendanceStatus.PRESENT }
    }
    val nonGatePresent = remember(session, nonGateStudents) {
        nonGateStudents.count { session.records[it.rollNumber]?.status == AttendanceStatus.PRESENT }
    }

    val absentees = remember(session, students) {
        students.filter { session.records[it.rollNumber]?.status != AttendanceStatus.PRESENT }
    }

    val lateComers = remember(session, students) {
        students.filter { session.records[it.rollNumber]?.isLate == true }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Session Name, Time & Sync Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = session.sessionName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = session.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Cloud Sync Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (session.isSynced) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synced",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cloud", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Local Only",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Local", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Stat Row: Total & Categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${session.presentCount}/${session.totalCount}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (session.absentCount == 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " Present (${session.getPresentPercentage()}%)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Mini Category Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEEF2FF)
                    ) {
                        Text(
                            text = "GATE: $gatePresent/${gateStudents.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4338CA),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF0F9FF)
                    ) {
                        Text(
                            text = "Non-GATE: $nonGatePresent/${nonGateStudents.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Quick Absentees or Late preview
            if (absentees.isNotEmpty() || lateComers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (absentees.isNotEmpty()) "❌ ${absentees.size} Absent" else "✅ All Present",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (absentees.isNotEmpty()) Color(0xFFBE123C) else Color(0xFF15803D)
                    )
                    if (lateComers.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ⏱️ ${lateComers.size} Late",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        if (absentees.isNotEmpty()) {
                            Text(
                                text = "Absentees:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            absentees.forEach { student ->
                                Text(
                                    text = "• [${if (student.isGate) "GATE" else "NON-GATE"}] ${student.rollNumber} - ${student.displayName}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE11D48)
                                )
                            }
                        }

                        if (lateComers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Late Comers:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            lateComers.forEach { student ->
                                Text(
                                    text = "• ⏱️ ${student.rollNumber} - ${student.displayName}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row: Quick Copy, Format Dialog, Edit Late Comers, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // One-tap direct copy button for teacher
                FilledTonalButton(
                    onClick = {
                        val report = session.formatTeacherReport(students)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Attendance Report", report)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "📋 Copied teacher report to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Msg", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Options/Share button (opens format picker with WhatsApp share)
                OutlinedButton(
                    onClick = onOpenFormatDialog,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 13.sp)
                }

                // Edit / Add Late Comers button
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit/Late", fontSize = 13.sp)
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Session",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
