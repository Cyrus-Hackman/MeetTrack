package com.meettrack.app.data.db

import android.util.Log
import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: String?): LocalDateTime? = value?.let {
        try {
            LocalDateTime.parse(it, formatter)
        } catch (e: DateTimeParseException) {
            Log.e("Converters", "Invalid stored LocalDateTime; using current time", e)
            LocalDateTime.now()
        }
    }

    @TypeConverter
    fun toLocalDateTime(date: LocalDateTime?): String? =
        date?.format(formatter)
}
