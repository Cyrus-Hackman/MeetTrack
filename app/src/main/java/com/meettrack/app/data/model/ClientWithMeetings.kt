package com.meettrack.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ClientWithMeetings(
    @Embedded val client: Client,
    @Relation(
        parentColumn = "id",
        entityColumn = "clientId"
    )
    val meetings: List<Meeting>
)
