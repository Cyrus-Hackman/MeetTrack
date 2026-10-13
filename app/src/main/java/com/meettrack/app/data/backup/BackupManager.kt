package com.meettrack.app.data.backup

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.JsonSerializationContext
import com.meettrack.app.data.db.MeetTrackDatabase
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.Meeting
import com.meettrack.app.data.model.MeetingStatus
import com.meettrack.app.data.repository.ClientRepository
import com.meettrack.app.data.repository.MeetingRepository
import com.meettrack.app.notifications.NotificationScheduler
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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
    private val notificationScheduler: NotificationScheduler
) {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
            override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
                return JsonPrimitive(src?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: "")
            }

            override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime {
                if (json == null || json.isJsonNull) {
                    throw JsonParseException("Missing LocalDateTime value in backup")
                }
                if (!json.isJsonPrimitive || !json.asJsonPrimitive.isString) {
                    throw JsonParseException("LocalDateTime value in backup must be an ISO-8601 string")
                }

                val str = json.asString
                return try {
                    LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                } catch (e: DateTimeParseException) {
                    throw JsonParseException(
                        "Invalid LocalDateTime in backup; expected ISO-8601 date and time",
                        e
                    )
                }
            }
        })
        .setPrettyPrinting()
        .create()

    suspend fun exportToJson(): String {
        val clients = clientRepository.getAllClientsDirect()
        val meetings = meetingRepository.getAllMeetingsDirect()
        val payload = BackupPayload(
            clients = clients,
            meetings = meetings
        )
        return gson.toJson(payload)
    }

    suspend fun exportClientsToCsv(): String {
        val clients = clientRepository.getAllClientsDirect()
        val sb = StringBuilder()
        sb.append("ID,Full Name,Phone Number,Email,Company,Address,Notes,Latitude,Longitude,Date Added\n")
        clients.forEach { c ->
            sb.append(listOf(
                c.id.toString(),
                escapeCsv(c.fullName),
                escapeCsv(c.phoneNumber),
                escapeCsv(c.email),
                escapeCsv(c.company),
                escapeCsv(c.address.ifBlank { c.formattedAddress }),
                escapeCsv(c.notes),
                c.latitude?.toString() ?: "",
                c.longitude?.toString() ?: "",
                c.dateAdded.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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
                m.id.toString(),
                escapeCsv(m.title),
                escapeCsv(clientName),
                m.dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                escapeCsv(m.location),
                escapeCsv(m.notes),
                m.status.name
            ).joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    suspend fun importFromJson(jsonString: String): Result<ImportSummary> {
        return try {
            val payload = gson.fromJson(jsonString, BackupPayload::class.java)
                ?: return Result.failure(IllegalArgumentException("Invalid backup JSON format"))

            val meetingIds = database.withTransaction {
                val availableClientIds = (
                    clientRepository.getAllClientsDirect().map { it.id } +
                        payload.clients.map { it.id }
                    ).toSet()
                val missingClientIds = payload.meetings
                    .map { it.clientId }
                    .filterNot { it in availableClientIds }
                    .distinct()
                require(missingClientIds.isEmpty()) {
                    "Backup contains meetings for missing client IDs: ${missingClientIds.joinToString()}"
                }

                if (payload.clients.isNotEmpty()) {
                    clientRepository.insertClients(payload.clients)
                }
                if (payload.meetings.isNotEmpty()) {
                    meetingRepository.insertMeetings(payload.meetings)
                } else {
                    emptyList()
                }
            }

            if (meetingIds.isNotEmpty()) {
                val now = LocalDateTime.now()
                meetingRepository.getMeetingsWithClientsByIds(meetingIds)
                    .filter {
                        it.meeting.status in setOf(MeetingStatus.UPCOMING, MeetingStatus.RESCHEDULED) &&
                            it.meeting.dateTime.isAfter(now)
                    }
                    .forEach { notificationScheduler.scheduleReminders(it) }
            }

            Result.success(ImportSummary(payload.clients.size, payload.meetings.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
