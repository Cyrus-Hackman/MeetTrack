package com.meettrack.app.data.db

import androidx.room.TypeConverter
import com.meettrack.app.data.model.MeetingStatus

class MeetingStatusConverter {
    @TypeConverter
    fun fromMeetingStatus(value: MeetingStatus): String = value.name

    @TypeConverter
    fun toMeetingStatus(value: String): MeetingStatus = MeetingStatus.valueOf(value)
}
