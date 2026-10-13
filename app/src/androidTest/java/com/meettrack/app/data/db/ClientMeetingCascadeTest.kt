package com.meettrack.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.Meeting
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ClientMeetingCascadeTest {

    @Test
    fun upsertingClientPreservesItsMeetings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(
            context,
            MeetTrackDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val clientDao = database.clientDao()
            val meetingDao = database.meetingDao()
            val client = Client(fullName = "Alex Client", phoneNumber = "555-0100")
            val clientId = clientDao.insertClient(client)
            val meetingId = meetingDao.insertMeeting(
                Meeting(
                    clientId = clientId,
                    title = "Follow-up",
                    dateTime = LocalDateTime.now().plusDays(1)
                )
            )

            clientDao.insertClient(client.copy(id = clientId, fullName = "Alex Updated"))

            assertEquals(listOf(meetingId), meetingDao.getAllMeetingsDirect().map { it.id })
        } finally {
            database.close()
        }
    }
}
