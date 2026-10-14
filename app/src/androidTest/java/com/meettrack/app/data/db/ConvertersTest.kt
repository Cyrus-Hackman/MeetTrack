package com.meettrack.app.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.meettrack.app.data.model.CORRUPT_DATE_SENTINEL
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ConvertersTest {

    @Test
    fun malformedStoredDateUsesMinimumSentinel() {
        assertEquals(CORRUPT_DATE_SENTINEL, Converters().fromLocalDateTime("corrupt-date"))
    }
}
