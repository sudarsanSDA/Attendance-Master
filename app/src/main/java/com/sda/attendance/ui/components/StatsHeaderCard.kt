package com.sda.attendance.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatsHeaderCard(
    presentCount: Int,
    absentCount: Int,
    totalCount: Int,
    gatePresent: Int,
    gateTotal: Int,
    nonGatePresent: Int,
    nonGateTotal: Int,
    modifier: Modifier = Modifier
) {
    val percentage = if (totalCount > 0) (presentCount * 100) / totalCount else 0
    val animatedProgress by animateFloatAsState(
        targetValue = if (totalCount > 0) presentCount.toFloat() / totalCount else 0f,
        label = "progress"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main Top Stat Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE ATTENDANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$presentCount",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = "/$totalCount Present",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                    }
                }

                // Percentage Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (percentage >= 75) Color(0xFFDCFCE7) else Color(0xFFFFE4E6)
                ) {
                    Text(
                        text = "$percentage%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (percentage >= 75) Color(0xFF15803D) else Color(0xFFBE123C),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF16A34A),
                trackColor = Color(0xFFFECDD3)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sub Categories: GATE vs NON-GATE & Absentees
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // GATE Stat Box
                CategoryStatBox(
                    title = "🎓 GATE",
                    present = gatePresent,
                    total = gateTotal,
                    bgColor = Color(0xFFEEF2FF),
                    textColor = Color(0xFF4338CA),
                    modifier = Modifier.weight(1f)
                )

                // Non-GATE Stat Box
                CategoryStatBox(
                    title = "📚 NON-GATE",
                    present = nonGatePresent,
                    total = nonGateTotal,
                    bgColor = Color(0xFFF0F9FF),
                    textColor = Color(0xFF0369A1),
                    modifier = Modifier.weight(1f)
                )

                // Absent Stat Box
                CategoryStatBox(
                    title = "❌ ABSENT",
                    present = absentCount,
                    total = totalCount,
                    isAbsentMode = true,
                    bgColor = Color(0xFFFFF1F2),
                    textColor = Color(0xFFBE123C),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CategoryStatBox(
    title: String,
    present: Int,
    total: Int,
    isAbsentMode: Boolean = false,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isAbsentMode) "$present" else "$present/$total",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
        }
    }
}
