package com.meettrack.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meettrack.app.data.model.MeetingWithClient
import com.meettrack.app.data.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentMonth = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val currentMonth: StateFlow<LocalDate> = _currentMonth.asStateFlow()

    val meetingsForSelectedDay: StateFlow<List<MeetingWithClient>> = _selectedDate
        .flatMapLatest { date ->
            val startOfDay = date.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endOfDay = date.atTime(23, 59, 59).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            meetingRepository.getMeetingsByDay(startOfDay, endOfDay)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val datesWithMeetings: StateFlow<Set<LocalDate>> = meetingRepository.getAllMeetingsWithClient()
        .map { meetings ->
            meetings.map { it.meeting.dateTime.toLocalDate() }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }
}
