package com.meettrack.app.notifications

import java.time.LocalDateTime

internal object ReminderTriggerTime {
    fun calculate(meetingDateTime: LocalDateTime, minutesBefore: Int): LocalDateTime =
        meetingDateTime.minusMinutes(minutesBefore.toLong())
}
