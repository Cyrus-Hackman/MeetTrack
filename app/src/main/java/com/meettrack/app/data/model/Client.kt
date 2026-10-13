package com.meettrack.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val phoneNumber: String,
    val email: String = "",
    val company: String = "",
    val address: String = "",
    val notes: String = "",
    val dateAdded: LocalDateTime = LocalDateTime.now(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val formattedAddress: String = "",
    val locationLastUpdated: LocalDateTime? = null
)
