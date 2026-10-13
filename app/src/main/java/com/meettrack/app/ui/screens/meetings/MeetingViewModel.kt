package com.meettrack.app.ui.screens.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.Meeting
import com.meettrack.app.data.model.MeetingStatus
import com.meettrack.app.data.model.MeetingWithClient
import com.meettrack.app.data.preferences.SettingsRepository
import com.meettrack.app.data.repository.ClientRepository
import com.meettrack.app.data.repository.MeetingRepository
import com.meettrack.app.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class MeetingFormState(
    val title: String = "",
    val selectedClient: Client? = null,
    val date: LocalDate = LocalDate.now().plusDays(1),
    val time: LocalTime = LocalTime.of(9, 0),
    val location: String = "",
    val notes: String = "",
    val reminderIntervals: List<Int> = listOf(60, 30),
    val status: MeetingStatus = MeetingStatus.UPCOMING,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MeetingViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val clientRepository: ClientRepository,
    private val notificationScheduler: NotificationScheduler,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val gson = Gson()

    private val _formState = MutableStateFlow(MeetingFormState())
    val formState: StateFlow<MeetingFormState> = _formState.asStateFlow()

    private val _meetingDetail = MutableStateFlow<MeetingWithClient?>(null)
    val meetingDetail: StateFlow<MeetingWithClient?> = _meetingDetail.asStateFlow()

    val allClients: StateFlow<List<Client>> = clientRepository.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val defaultReminderMinutes = settingsRepository.settings.first().defaultReminderMinutes
            if (_formState.value == MeetingFormState()) {
                _formState.value = _formState.value.copy(
                    reminderIntervals = defaultReminderMinutes
                )
            }
        }
    }

    fun loadMeeting(meetingId: Long) {
        viewModelScope.launch {
            meetingRepository.getMeetingWithClientById(meetingId).collect { meetingWithClient ->
                _meetingDetail.value = meetingWithClient
                meetingWithClient?.let { mwc ->
                    val intervals = parseIntervals(mwc.meeting.reminderIntervals)
                    _formState.value = MeetingFormState(
                        title = mwc.meeting.title,
                        selectedClient = mwc.client,
                        date = mwc.meeting.dateTime.toLocalDate(),
                        time = mwc.meeting.dateTime.toLocalTime(),
                        location = mwc.meeting.location,
                        notes = mwc.meeting.notes,
                        reminderIntervals = intervals,
                        status = mwc.meeting.status
                    )
                }
            }
        }
    }

    fun preselectClient(clientId: Long) {
        viewModelScope.launch {
            clientRepository.getClientById(clientId).collect { client ->
                if (client != null) {
                    _formState.value = _formState.value.copy(
                        selectedClient = client,
                        location = client.formattedAddress.ifBlank { client.address }
                    )
                }
            }
        }
    }

    fun updateTitle(value: String) { _formState.value = _formState.value.copy(title = value) }
    fun updateClient(client: Client) {
        _formState.value = _formState.value.copy(
            selectedClient = client,
            location = _formState.value.location.ifBlank {
                client.formattedAddress.ifBlank { client.address }
            }
        )
    }
    fun updateDate(value: LocalDate) { _formState.value = _formState.value.copy(date = value) }
    fun updateTime(value: LocalTime) { _formState.value = _formState.value.copy(time = value) }
    fun updateLocation(value: String) { _formState.value = _formState.value.copy(location = value) }
    fun updateNotes(value: String) { _formState.value = _formState.value.copy(notes = value) }
    fun toggleReminderInterval(minutes: Int) {
        val current = _formState.value.reminderIntervals.toMutableList()
        if (current.contains(minutes)) current.remove(minutes) else current.add(minutes)
        _formState.value = _formState.value.copy(reminderIntervals = current.sorted())
    }
    fun updateStatus(status: MeetingStatus) { _formState.value = _formState.value.copy(status = status) }

    fun saveNewMeeting(onSuccess: (Long) -> Unit) {
        val state = _formState.value
        if (state.title.isBlank()) {
            _formState.value = state.copy(error = "Meeting title is required")
            return
        }
        if (state.selectedClient == null) {
            _formState.value = state.copy(error = "Please select a client")
            return
        }
        viewModelScope.launch {
            _formState.value = state.copy(isLoading = true, error = null)
            try {
                val meeting = Meeting(
                    clientId = state.selectedClient.id,
                    title = state.title.trim(),
                    dateTime = LocalDateTime.of(state.date, state.time),
                    location = state.location.trim(),
                    notes = state.notes.trim(),
                    reminderIntervals = gson.toJson(state.reminderIntervals),
                    status = state.status
                )
                val id = meetingRepository.addMeeting(meeting)
                // Schedule reminders
                val savedMeeting = meetingRepository.getMeetingById(id)
                savedMeeting?.let {
                    notificationScheduler.scheduleReminders(
                        MeetingWithClient(it, state.selectedClient)
                    )
                }
                _formState.value = _formState.value.copy(isLoading = false, isSaved = true)
                onSuccess(id)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    error = "Failed to save meeting: ${e.message}"
                )
            }
        }
    }

    fun updateExistingMeeting(meetingId: Long, onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.title.isBlank()) {
            _formState.value = state.copy(error = "Meeting title is required")
            return
        }
        if (state.selectedClient == null) {
            _formState.value = state.copy(error = "Please select a client")
            return
        }
        viewModelScope.launch {
            _formState.value = state.copy(isLoading = true, error = null)
            try {
                val meeting = Meeting(
                    id = meetingId,
                    clientId = state.selectedClient.id,
                    title = state.title.trim(),
                    dateTime = LocalDateTime.of(state.date, state.time),
                    location = state.location.trim(),
                    notes = state.notes.trim(),
                    reminderIntervals = gson.toJson(state.reminderIntervals),
                    status = state.status,
                    updatedAt = LocalDateTime.now()
                )
                meetingRepository.updateMeeting(meeting)
                notificationScheduler.scheduleReminders(
                    MeetingWithClient(meeting, state.selectedClient)
                )
                _formState.value = _formState.value.copy(isLoading = false, isSaved = true)
                onSuccess()
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    error = "Failed to update meeting: ${e.message}"
                )
            }
        }
    }

    fun cancelMeeting(meetingId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            meetingRepository.updateMeetingStatus(meetingId, MeetingStatus.CANCELLED)
            notificationScheduler.cancelReminders(meetingId)
            onSuccess()
        }
    }

    fun completeMeeting(meetingId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            meetingRepository.updateMeetingStatus(meetingId, MeetingStatus.COMPLETED)
            notificationScheduler.cancelReminders(meetingId)
            onSuccess()
        }
    }

    fun deleteMeeting(meetingId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            notificationScheduler.cancelReminders(meetingId)
            meetingRepository.deleteMeetingById(meetingId)
            onSuccess()
        }
    }

    fun clearError() { _formState.value = _formState.value.copy(error = null) }

    private fun parseIntervals(json: String): List<Int> {
        return try {
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}
