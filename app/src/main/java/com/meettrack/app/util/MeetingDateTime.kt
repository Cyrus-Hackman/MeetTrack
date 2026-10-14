package com.meettrack.app.util

import com.meettrack.app.data.model.CORRUPT_DATE_SENTINEL
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun LocalDateTime.formatMeetingDateTime(formatter: DateTimeFormatter): String =
    if (this == CORRUPT_DATE_SENTINEL) "Invalid date" else format(formatter)

internal fun LocalDateTime.toEpochMillisOrNull(zoneId: ZoneId = ZoneId.systemDefault()): Long? {
    if (this == CORRUPT_DATE_SENTINEL) return null
    return try {
        atZone(zoneId).toInstant().toEpochMilli()
    } catch (_: ArithmeticException) {
        null
    } catch (_: DateTimeException) {
        null
    }
}
