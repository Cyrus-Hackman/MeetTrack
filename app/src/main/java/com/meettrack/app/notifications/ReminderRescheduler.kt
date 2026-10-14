package com.meettrack.app.notifications

import com.meettrack.app.data.model.MeetingWithClient

interface ReminderRescheduler {
    suspend fun rescheduleUpcoming(meetings: List<MeetingWithClient>)
}
