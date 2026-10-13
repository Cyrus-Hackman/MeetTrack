package com.meettrack.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.MeetingWithClient
import com.meettrack.app.data.repository.ClientRepository
import com.meettrack.app.data.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val clientCount: Int = 0,
    val upcomingMeetingCount: Int = 0,
    val todaysMeetings: List<MeetingWithClient> = emptyList(),
    val upcomingMeetings: List<MeetingWithClient> = emptyList(),
    val overdueMeetings: List<MeetingWithClient> = emptyList(),
    val recentClients: List<Client> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    clientRepository: ClientRepository,
    meetingRepository: MeetingRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        clientRepository.getClientCount(),
        meetingRepository.getUpcomingMeetingCount(),
        meetingRepository.getTodaysMeetings(),
        meetingRepository.getUpcomingMeetings(),
        meetingRepository.getOverdueMeetings(),
        clientRepository.getRecentClients(5)
    ) { values ->
        val clientCount = values[0] as Int
        val upcomingCount = values[1] as Int
        @Suppress("UNCHECKED_CAST")
        val todaysMeetings = values[2] as List<MeetingWithClient>
        @Suppress("UNCHECKED_CAST")
        val upcomingMeetings = values[3] as List<MeetingWithClient>
        @Suppress("UNCHECKED_CAST")
        val overdueMeetings = values[4] as List<MeetingWithClient>
        @Suppress("UNCHECKED_CAST")
        val recentClients = values[5] as List<Client>

        DashboardUiState(
            clientCount = clientCount,
            upcomingMeetingCount = upcomingCount,
            todaysMeetings = todaysMeetings,
            upcomingMeetings = upcomingMeetings.take(5),
            overdueMeetings = overdueMeetings,
            recentClients = recentClients,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
