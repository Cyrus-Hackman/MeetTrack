package com.meettrack.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.Meeting
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

@RunWith(AndroidJUnit4::class)
class ClientMeetingCascadeTest {

    @Test
    fun upsertingClientPreservesItsMeetings() = runBlocking {
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

    @Test
    fun corruptDateIsExcludedFromUpcomingAndIncludedInOverdue() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(
            context,
            MeetTrackDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val clientDao = database.clientDao()
            val meetingDao = database.meetingDao()
            val clientId = clientDao.insertClient(
                Client(fullName = "Date Test Client", phoneNumber = "555-0100")
            )
            val meetingId = meetingDao.insertMeeting(
                Meeting(
                    clientId = clientId,
                    title = "Corrupt date",
                    dateTime = LocalDateTime.now().plusDays(1)
                )
            )
            database.openHelper.writableDatabase.execSQL(
                "UPDATE meetings SET dateTime = ? WHERE id = ?",
                arrayOf<Any>("corrupt-date", meetingId)
            )

            val upcomingIds = meetingDao.getUpcomingMeetings().first().map { it.meeting.id }
            val overdueIds = meetingDao.getOverdueMeetings().first().map { it.meeting.id }

            assertFalse(meetingId in upcomingIds)
            assertTrue(meetingId in overdueIds)
            assertEquals(0, meetingDao.getUpcomingMeetingCount().first())
        } finally {
            database.close()
        }
    }
}
