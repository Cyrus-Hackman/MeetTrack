package com.meettrack.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

val CORRUPT_DATE_SENTINEL: LocalDateTime = LocalDateTime.MIN

enum class MeetingStatus { UPCOMING, COMPLETED, CANCELLED, RESCHEDULED }

@Entity(
    tableName = "meetings",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId")]
)
data class Meeting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long,
    val title: String,
    val dateTime: LocalDateTime,
    val location: String = "",
    val notes: String = "",
    val status: MeetingStatus = MeetingStatus.UPCOMING,
    val reminderIntervals: String = "[]", // JSON array of minutes before: [60, 30, 10]
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
