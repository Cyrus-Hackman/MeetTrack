package com.meettrack.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.meettrack.app.ui.navigation.MeetTrackNavHost
import com.meettrack.app.ui.navigation.Screen
import com.meettrack.app.ui.screens.settings.SettingsViewModel
import com.meettrack.app.ui.theme.MeetTrackTheme
import dagger.hilt.android.AndroidEntryPoint

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Home", Icons.Default.Home),
    BottomNavItem(Screen.Clients, "Clients", Icons.Default.PeopleOutline, Icons.Default.People),
    BottomNavItem(Screen.Calendar, "Calendar", Icons.Default.CalendarMonth),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings, Icons.Default.Settings)
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            val darkTheme = when {
                settings.useSystemTheme -> isSystemInDarkTheme()
                else -> settings.useDarkTheme
            }

            MeetTrackTheme(darkTheme = darkTheme) {
                // Request notification permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notifPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
                    LaunchedEffect(Unit) {
                        if (!notifPermission.status.isGranted) {
                            notifPermission.launchPermissionRequest()
                        }
                    }
                }

                MeetTrackApp(
                    navigateToMeetingId = intent?.getLongExtra("meeting_id", -1L)
                        ?.takeIf { it != -1L }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetTrackApp(navigateToMeetingId: Long? = null) {
    val navController = rememberNavController()

    // Deep link handling from notification tap
    LaunchedEffect(navigateToMeetingId) {
        navigateToMeetingId?.let { id ->
            navController.navigate(Screen.MeetingDetail.createRoute(id))
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        MeetTrackNavHost(
            navController = navController,
            paddingValues = paddingValues
        )
    }
}
