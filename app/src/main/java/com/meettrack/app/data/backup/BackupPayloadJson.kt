package com.meettrack.app.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.Meeting
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal object BackupPayloadJson {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(
            LocalDateTime::class.java,
            object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
                override fun serialize(
                    src: LocalDateTime?,
                    typeOfSrc: Type?,
                    context: JsonSerializationContext?
                ): JsonElement =
                    src?.let { JsonPrimitive(it.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)) }
                        ?: JsonNull.INSTANCE

                override fun deserialize(
                    json: JsonElement?,
                    typeOfT: Type?,
                    context: JsonDeserializationContext?
                ): LocalDateTime {
                    if (json == null || json.isJsonNull) {
                        throw JsonParseException("Missing LocalDateTime value in backup")
                    }
                    if (!json.isJsonPrimitive || !json.asJsonPrimitive.isString) {
                        throw JsonParseException("LocalDateTime value in backup must be an ISO-8601 string")
                    }

                    return try {
                        LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    } catch (e: DateTimeParseException) {
                        throw JsonParseException(
                            "Invalid LocalDateTime in backup; expected ISO-8601 date and time",
                            e
                        )
                    }
                }
            }
        )
        .setPrettyPrinting()
        .create()

    fun encode(payload: BackupPayload): String = gson.toJson(payload)

    fun decode(json: String): BackupPayload {
        validateDateFields(JsonParser.parseString(json))
        return gson.fromJson(json, BackupPayload::class.java)
            ?: throw JsonParseException("Invalid backup JSON format")
    }

    fun requireValidClientReferences(
        existingClientIds: Set<Long>,
        clients: List<Client>,
        meetings: List<Meeting>
    ) {
        val availableClientIds = existingClientIds + clients.map { it.id }
        val missingClientIds = meetings
            .map { it.clientId }
            .filterNot { it in availableClientIds }
            .distinct()
        if (missingClientIds.isNotEmpty()) {
            throw IllegalArgumentException(
                "Backup contains meetings for missing client IDs: ${missingClientIds.joinToString()}"
            )
        }
    }

    private fun validateDateFields(root: JsonElement) {
        if (!root.isJsonObject) return

        val payload = root.asJsonObject
        validateDateFields(payload.get("clients"), "clients", listOf("dateAdded"))
        validateDateFields(
            payload.get("meetings"),
            "meetings",
            listOf("dateTime", "createdAt", "updatedAt")
        )
    }

    private fun validateDateFields(
        entities: JsonElement?,
        collectionName: String,
        fields: List<String>
    ) {
        if (entities?.isJsonArray != true) return

        entities.asJsonArray.forEachIndexed { index, entity ->
            if (!entity.isJsonObject) return@forEachIndexed

            fields.forEach { field ->
                val value = entity.asJsonObject.get(field)
                if (value == null || value.isJsonNull || !value.isJsonPrimitive ||
                    !value.asJsonPrimitive.isString
                ) {
                    throw JsonParseException(
                        "Missing or invalid $collectionName[$index].$field in backup"
                    )
                }
            }
        }
    }
}
