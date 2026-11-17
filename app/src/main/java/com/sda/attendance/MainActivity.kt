package com.sda.attendance

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.sda.attendance.ui.screens.AnalyticsScreen
import com.sda.attendance.ui.screens.HistoryScreen
import com.sda.attendance.ui.screens.SettingsScreen
import com.sda.attendance.ui.screens.TakeAttendanceScreen
import com.sda.attendance.ui.theme.AttendanceTheme
import com.sda.attendance.ui.viewmodel.AttendanceViewModel

enum class NavigationTab(val title: String, val icon: ImageVector) {
    ATTENDANCE("Attendance", Icons.Default.AssignmentTurnedIn),
    HISTORY("Logs", Icons.Default.History),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AttendanceTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: AttendanceViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavigationTab.ATTENDANCE) }
    val userMessage by viewModel.userMessage.collectAsState()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "screenTransition") { tab ->
                when (tab) {
                    NavigationTab.ATTENDANCE -> TakeAttendanceScreen(viewModel = viewModel)
                    NavigationTab.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        onNavigateToTakeAttendance = { currentTab = NavigationTab.ATTENDANCE }
                    )
                    NavigationTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                    NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
