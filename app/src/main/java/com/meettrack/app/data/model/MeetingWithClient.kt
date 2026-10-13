package com.meettrack.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MeetingWithClient(
    @Embedded val meeting: Meeting,
    @Relation(
        parentColumn = "clientId",
        entityColumn = "id"
    )
    val client: Client
)
