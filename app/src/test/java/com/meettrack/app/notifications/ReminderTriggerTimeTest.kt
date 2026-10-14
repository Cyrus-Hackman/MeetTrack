package com.meettrack.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ReminderTriggerTimeTest {

    @Test
    fun calculatesTriggerTimeMinutesBeforeMeeting() {
        val meetingTime = LocalDateTime.of(2026, 10, 14, 15, 0)

        assertEquals(
            LocalDateTime.of(2026, 10, 14, 14, 30),
            ReminderTriggerTime.calculate(meetingTime, 30)
        )
    }
}
