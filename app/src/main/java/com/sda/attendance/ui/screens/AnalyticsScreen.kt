package com.sda.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sda.attendance.data.model.AttendanceStatus
import com.sda.attendance.ui.viewmodel.AttendanceViewModel

@Composable
fun AnalyticsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val students by viewModel.students.collectAsState()

    val totalSessions = sessions.size

    // Calculate per-student attendance
    val studentStats = remember(sessions, students) {
        students.map { student ->
            val presentCount = sessions.count { session ->
                session.records[student.rollNumber]?.status == AttendanceStatus.PRESENT
            }
            val lateCount = sessions.count { session ->
                session.records[student.rollNumber]?.isLate == true
            }
            val percentage = if (totalSessions > 0) (presentCount * 100) / totalSessions else 100
            Triple(student, presentCount, percentage) to lateCount
        }.sortedBy { it.first.third } // lowest attendance first to spot absentees easily
    }

    val overallAvg = remember(studentStats, totalSessions) {
        if (studentStats.isNotEmpty() && totalSessions > 0) {
            studentStats.map { it.first.third }.average().toInt()
        } else 100
    }

    val gateStudents = remember(students) { students.filter { it.isGate } }
    val nonGateStudents = remember(students) { students.filter { !it.isGate } }

    val gateAvg = remember(gateStudents, sessions, totalSessions) {
        if (gateStudents.isNotEmpty() && totalSessions > 0) {
            val totalPresent = gateStudents.sumOf { s ->
                sessions.count { it.records[s.rollNumber]?.status == AttendanceStatus.PRESENT }
            }
            (totalPresent * 100) / (gateStudents.size * totalSessions)
        } else 100
    }

    val nonGateAvg = remember(nonGateStudents, sessions, totalSessions) {
        if (nonGateStudents.isNotEmpty() && totalSessions > 0) {
            val totalPresent = nonGateStudents.sumOf { s ->
                sessions.count { it.records[s.rollNumber]?.status == AttendanceStatus.PRESENT }
            }
            (totalPresent * 100) / (nonGateStudents.size * totalSessions)
        } else 100
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Attendance Analytics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Overall student statistics across all $totalSessions sessions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Overall Summary Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CLASS AVERAGE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$overallAvg%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text("Total Sessions: $totalSessions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Students: ${students.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎓 GATE Average", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4338CA))
                                Text("$gateAvg%", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4338CA))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0F9FF),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📚 Non-GATE Average", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                                Text("$nonGateAvg%", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0369A1))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Student-wise Attendance (75% Minimum Target)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(studentStats, key = { it.first.first.rollNumber }) { (pair, lateCount) ->
            val (student, present, percentage) = pair
            val isBelowTarget = percentage < 75 && totalSessions > 0

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isBelowTarget) Color(0xFFFFF1F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = if (isBelowTarget) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDA4AF)) else null,
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
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (student.isGate) Color(0xFF4F46E5) else Color(0xFF0284C7)
                            ) {
                                Text(
                                    text = if (student.isGate) "GATE" else "NON-GATE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            if (isBelowTarget) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚠️ <75%", color = Color(0xFFE11D48), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = student.displayName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { if (totalSessions > 0) percentage / 100f else 1f },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (percentage >= 75) Color(0xFF16A34A) else Color(0xFFE11D48),
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$percentage%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (percentage >= 75) Color(0xFF16A34A) else Color(0xFFBE123C)
                        )
                        Text(
                            text = "$present/$totalSessions attended",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
