package com.meettrack.app.util

import com.meettrack.app.data.model.CORRUPT_DATE_SENTINEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MeetingDateTimeTest {

    @Test
    fun corruptDateSentinelFormatsAsPlaceholder() {
        assertEquals(
            "Invalid date",
            CORRUPT_DATE_SENTINEL.formatMeetingDateTime(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    @Test
    fun corruptDateSentinelCannotConvertToEpochMillis() {
        assertNull(CORRUPT_DATE_SENTINEL.toEpochMillisOrNull(ZoneOffset.UTC))
    }

    @Test
    fun dateOutsideEpochMillisRangeCannotConvert() {
        assertNull(LocalDateTime.MAX.toEpochMillisOrNull(ZoneOffset.UTC))
    }
}
