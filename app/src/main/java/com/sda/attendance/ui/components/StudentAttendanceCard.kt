package com.sda.attendance.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sda.attendance.data.model.AttendanceStatus
import com.sda.attendance.data.model.Student

@Composable
fun StudentAttendanceCard(
    student: Student,
    status: AttendanceStatus,
    isLate: Boolean,
    onToggleStatus: () -> Unit,
    onToggleLate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPresent = status == AttendanceStatus.PRESENT

    // Animated dynamic colors for smooth, friendly visual feedback
    val cardBgColor by animateColorAsState(
        targetValue = if (isPresent) Color(0xFFF0FDF4) else Color(0xFFFFF1F2),
        animationSpec = tween(200),
        label = "cardBgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isPresent) Color(0xFF22C55E) else Color(0xFFF43F5E),
        animationSpec = tween(200),
        label = "borderColor"
    )

    val statusPillBg by animateColorAsState(
        targetValue = if (isPresent) Color(0xFF16A34A) else Color(0xFFE11D48),
        animationSpec = tween(200),
        label = "statusPillBg"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBgColor,
        border = BorderStroke(1.5.dp, borderColor.copy(alpha = 0.5f)),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onToggleStatus)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Status Indicator Circle / Button
            Surface(
                shape = CircleShape,
                color = statusPillBg,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPresent) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (isPresent) "Present" else "Absent",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Student Info (Roll No, Name, Tags)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = student.rollNumber,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // GATE or NON-GATE Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (student.isGate) Color(0xFF4F46E5) else Color(0xFF0284C7)
                    ) {
                        Text(
                            text = if (student.isGate) "🎓 GATE" else "📚 NON-GATE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Late Comer Badge
                    if (isLate && isPresent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFD97706)
                        ) {
                            Text(
                                text = "⏱️ LATE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = student.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )

                if (student.fullName != student.displayName && student.fullName.isNotBlank()) {
                    Text(
                        text = student.fullName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            // Right Action Buttons: Late toggle and Phone
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Late comer toggle button
                IconButton(
                    onClick = onToggleLate,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Toggle Late",
                        tint = if (isLate) Color(0xFFD97706) else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Call button if phone exists
                if (student.phoneNumber.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${student.phoneNumber}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call Student",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
