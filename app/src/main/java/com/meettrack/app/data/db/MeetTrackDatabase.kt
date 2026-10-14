package com.meettrack.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.Meeting

@Database(
    entities = [Client::class, Meeting::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class, MeetingStatusConverter::class)
abstract class MeetTrackDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun meetingDao(): MeetingDao
}
