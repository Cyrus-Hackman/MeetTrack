package com.meettrack.app.data.backup

import androidx.room.withTransaction
import com.meettrack.app.data.db.MeetTrackDatabase
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.Meeting
import com.meettrack.app.data.repository.ClientRepository
import com.meettrack.app.data.repository.MeetingRepository
import com.meettrack.app.notifications.ReminderRescheduler
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class BackupPayload(
    val appVersion: String = "1.0",
    val exportedAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val clients: List<Client>,
    val meetings: List<Meeting>
)

data class ImportSummary(
    val clientsCount: Int,
    val meetingsCount: Int
)

@Singleton
class BackupManager @Inject constructor(
    private val database: MeetTrackDatabase,
    private val clientRepository: ClientRepository,
    private val meetingRepository: MeetingRepository,
    private val reminderRescheduler: ReminderRescheduler
) {
    suspend fun exportToJson(): String {
        val clients = clientRepository.getAllClientsDirect()
        val meetings = meetingRepository.getAllMeetingsDirect()
        val payload = BackupPayload(
            clients = clients,
            meetings = meetings
        )
        return BackupPayloadJson.encode(payload)
    }

    suspend fun exportClientsToCsv(): String {
        val clients = clientRepository.getAllClientsDirect()
        val sb = StringBuilder()
        sb.append("ID,Full Name,Phone Number,Email,Company,Address,Notes,Latitude,Longitude,Date Added\n")
        clients.forEach { c ->
            sb.append(listOf(
                escapeCsv(c.id.toString()),
                escapeCsv(c.fullName),
                escapeCsv(c.phoneNumber),
                escapeCsv(c.email),
                escapeCsv(c.company),
                escapeCsv(c.address.ifBlank { c.formattedAddress }),
                escapeCsv(c.notes),
                escapeCsv(c.latitude?.toString() ?: ""),
                escapeCsv(c.longitude?.toString() ?: ""),
                escapeCsv(c.dateAdded.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            ).joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    suspend fun exportMeetingsToCsv(): String {
        val clients = clientRepository.getAllClientsDirect().associateBy { it.id }
        val meetings = meetingRepository.getAllMeetingsDirect()
        val sb = StringBuilder()
        sb.append("ID,Title,Client Name,Date Time,Location,Notes,Status\n")
        meetings.forEach { m ->
            val clientName = clients[m.clientId]?.fullName ?: "Unknown Client"
            sb.append(listOf(
                escapeCsv(m.id.toString()),
                escapeCsv(m.title),
                escapeCsv(clientName),
                escapeCsv(m.dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                escapeCsv(m.location),
                escapeCsv(m.notes),
                escapeCsv(m.status.name)
            ).joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    suspend fun importFromJson(jsonString: String): Result<ImportSummary> {
        return try {
            val payload = BackupPayloadJson.decode(jsonString)

            database.withTransaction {
                BackupPayloadJson.requireValidClientReferences(
                    clientRepository.getAllClientsDirect().map { it.id }.toSet(),
                    payload.clients,
                    payload.meetings
                )

                if (payload.clients.isNotEmpty()) {
                    clientRepository.insertClients(payload.clients)
                }
                if (payload.meetings.isNotEmpty()) {
                    meetingRepository.insertMeetings(payload.meetings)
                }
            }

            if (payload.meetings.isNotEmpty()) {
                reminderRescheduler.rescheduleUpcoming(
                    meetingRepository.getMeetingsWithClientsByIds(payload.meetings.map { it.id })
                )
            }

            Result.success(ImportSummary(payload.clients.size, payload.meetings.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        val safeValue = when (value.firstOrNull()) {
            '=', '+', '-', '@', '\t', '\r' -> "'$value"
            else -> value
        }
        return if (safeValue.contains(",") || safeValue.contains("\"") ||
            safeValue.contains("\n") || safeValue.contains("\r")
        ) {
            "\"" + safeValue.replace("\"", "\"\"") + "\""
        } else {
            safeValue
        }
    }
}
