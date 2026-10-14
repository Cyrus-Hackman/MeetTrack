package com.meettrack.app.data.backup

import com.google.gson.JsonParseException
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.Meeting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDateTime

class BackupPayloadJsonTest {

    @Test
    fun encodesAndDecodesBackupPayload() {
        val client = Client(
            id = 7,
            fullName = "Jamie Example",
            phoneNumber = "555-0107"
        )
        val meeting = Meeting(
            id = 11,
            clientId = client.id,
            title = "Project review",
            dateTime = LocalDateTime.of(2026, 10, 20, 14, 30)
        )
        val payload = BackupPayload(
            exportedAt = "2026-10-14T08:00:00",
            clients = listOf(client),
            meetings = listOf(meeting)
        )

        assertEquals(payload, BackupPayloadJson.decode(BackupPayloadJson.encode(payload)))
    }

    @Test
    fun rejectsInvalidMeetingDate() {
        val json = BackupPayloadJson.encode(
            BackupPayload(
                clients = emptyList(),
                meetings = listOf(
                    Meeting(
                        clientId = 1,
                        title = "Bad date",
                        dateTime = LocalDateTime.of(2026, 10, 20, 14, 30)
                    )
                )
            )
        ).replace("2026-10-20T14:30:00", "not-a-date")

        assertThrows(JsonParseException::class.java) {
            BackupPayloadJson.decode(json)
        }
    }

    @Test
    fun rejectsMeetingReferencesMissingClient() {
        val meeting = Meeting(
            clientId = 404,
            title = "Orphan",
            dateTime = LocalDateTime.of(2026, 10, 20, 14, 30)
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupPayloadJson.requireValidClientReferences(
                existingClientIds = emptySet(),
                clients = emptyList(),
                meetings = listOf(meeting)
            )
        }

        assertEquals(
            "Backup contains meetings for missing client IDs: 404",
            error.message
        )
    }
}
