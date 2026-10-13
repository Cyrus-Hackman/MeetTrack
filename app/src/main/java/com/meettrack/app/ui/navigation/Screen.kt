package com.meettrack.app.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Clients : Screen("clients")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")

    object ClientDetail : Screen("client/{clientId}") {
        fun createRoute(clientId: Long) = "client/$clientId"
    }
    object AddClient : Screen("add_client")
    object EditClient : Screen("edit_client/{clientId}") {
        fun createRoute(clientId: Long) = "edit_client/$clientId"
    }

    object AddMeeting : Screen("add_meeting?clientId={clientId}") {
        fun createRoute(clientId: Long? = null) =
            if (clientId != null) "add_meeting?clientId=$clientId" else "add_meeting"
    }
    object EditMeeting : Screen("edit_meeting/{meetingId}") {
        fun createRoute(meetingId: Long) = "edit_meeting/$meetingId"
    }
    object MeetingDetail : Screen("meeting/{meetingId}") {
        fun createRoute(meetingId: Long) = "meeting/$meetingId"
    }

    object ClientMap : Screen("client_map/{clientId}") {
        fun createRoute(clientId: Long) = "client_map/$clientId"
    }
}
