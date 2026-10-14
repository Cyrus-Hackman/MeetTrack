package com.meettrack.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.meettrack.app.ui.screens.calendar.CalendarScreen
import com.meettrack.app.ui.screens.clients.AddClientScreen
import com.meettrack.app.ui.screens.clients.ClientDetailScreen
import com.meettrack.app.ui.screens.clients.ClientListScreen
import com.meettrack.app.ui.screens.clients.EditClientScreen
import com.meettrack.app.ui.screens.dashboard.DashboardScreen
import com.meettrack.app.ui.screens.map.ClientMapScreen
import com.meettrack.app.ui.screens.meetings.AddMeetingScreen
import com.meettrack.app.ui.screens.meetings.EditMeetingScreen
import com.meettrack.app.ui.screens.meetings.MeetingDetailScreen
import com.meettrack.app.ui.screens.settings.SettingsScreen

@Composable
fun MeetTrackNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        enterTransition = {
            fadeIn(tween(220)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start, tween(220)
            )
        },
        exitTransition = {
            fadeOut(tween(220)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start, tween(220)
            )
        },
        popEnterTransition = {
            fadeIn(tween(220)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End, tween(220)
            )
        },
        popExitTransition = {
            fadeOut(tween(220)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End, tween(220)
            )
        }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                paddingValues = paddingValues,
                onNavigateToClients = { navController.navigate(Screen.Clients.route) },
                onNavigateToAddClient = { navController.navigate(Screen.AddClient.route) },
                onNavigateToAddMeeting = { navController.navigate(Screen.AddMeeting.createRoute()) },
                onNavigateToMeeting = { id -> navController.navigate(Screen.MeetingDetail.createRoute(id)) },
                onNavigateToClient = { id -> navController.navigate(Screen.ClientDetail.createRoute(id)) }
            )
        }

        composable(Screen.Clients.route) {
            ClientListScreen(
                paddingValues = paddingValues,
                onNavigateToClient = { id -> navController.navigate(Screen.ClientDetail.createRoute(id)) },
                onNavigateToAddClient = { navController.navigate(Screen.AddClient.route) }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                paddingValues = paddingValues,
                onNavigateToMeeting = { id -> navController.navigate(Screen.MeetingDetail.createRoute(id)) },
                onNavigateToAddMeeting = { navController.navigate(Screen.AddMeeting.createRoute()) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(paddingValues = paddingValues)
        }

        composable(Screen.AddClient.route) {
            AddClientScreen(
                onNavigateBack = { navController.popBackStack() },
                onClientAdded = { id ->
                    navController.popBackStack()
                    navController.navigate(Screen.ClientDetail.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.ClientDetail.route,
            arguments = listOf(navArgument("clientId") { type = NavType.LongType })
        ) { backStack ->
            val clientId = backStack.arguments?.getLong("clientId") ?: return@composable
            ClientDetailScreen(
                clientId = clientId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Screen.EditClient.createRoute(clientId)) },
                onNavigateToAddMeeting = { navController.navigate(Screen.AddMeeting.createRoute(clientId)) },
                onNavigateToMeeting = { id -> navController.navigate(Screen.MeetingDetail.createRoute(id)) },
                onNavigateToMap = { navController.navigate(Screen.ClientMap.createRoute(clientId)) }
            )
        }

        composable(
            route = Screen.EditClient.route,
            arguments = listOf(navArgument("clientId") { type = NavType.LongType })
        ) { backStack ->
            val clientId = backStack.arguments?.getLong("clientId") ?: return@composable
            EditClientScreen(
                clientId = clientId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddMeeting.route,
            arguments = listOf(
                navArgument("clientId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStack ->
            val clientId = backStack.arguments?.getLong("clientId")?.takeIf { it != -1L }
            AddMeetingScreen(
                preselectedClientId = clientId,
                onNavigateBack = { navController.popBackStack() },
                onMeetingAdded = { id ->
                    navController.popBackStack()
                    navController.navigate(Screen.MeetingDetail.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.EditMeeting.route,
            arguments = listOf(navArgument("meetingId") { type = NavType.LongType })
        ) { backStack ->
            val meetingId = backStack.arguments?.getLong("meetingId") ?: return@composable
            EditMeetingScreen(
                meetingId = meetingId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MeetingDetail.route,
            arguments = listOf(navArgument("meetingId") { type = NavType.LongType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "meettrack://meeting/{meetingId}" }
            )
        ) { backStack ->
            val meetingId = backStack.arguments?.getLong("meetingId") ?: return@composable
            MeetingDetailScreen(
                meetingId = meetingId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Screen.EditMeeting.createRoute(meetingId)) },
                onNavigateToClient = { id -> navController.navigate(Screen.ClientDetail.createRoute(id)) }
            )
        }

        composable(
            route = Screen.ClientMap.route,
            arguments = listOf(navArgument("clientId") { type = NavType.LongType })
        ) { backStack ->
            val clientId = backStack.arguments?.getLong("clientId") ?: return@composable
            ClientMapScreen(
                clientId = clientId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
