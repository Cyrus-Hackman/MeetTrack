package com.meettrack.app.data.repository

import com.meettrack.app.data.db.MeetingDao
import com.meettrack.app.data.model.Meeting
import com.meettrack.app.data.model.MeetingStatus
import com.meettrack.app.data.model.MeetingWithClient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepository @Inject constructor(
    private val meetingDao: MeetingDao
) {
    fun getAllMeetingsWithClient(): Flow<List<MeetingWithClient>> =
        meetingDao.getAllMeetingsWithClient()

    fun getMeetingsForClient(clientId: Long): Flow<List<MeetingWithClient>> =
        meetingDao.getMeetingsForClient(clientId)
    suspend fun getMeetingIdsForClient(clientId: Long): List<Long> =
        meetingDao.getMeetingIdsForClient(clientId)

    fun getTodaysMeetings(): Flow<List<MeetingWithClient>> = meetingDao.getTodaysMeetings()
    fun getUpcomingMeetings(): Flow<List<MeetingWithClient>> = meetingDao.getUpcomingMeetings()
    fun getOverdueMeetings(): Flow<List<MeetingWithClient>> = meetingDao.getOverdueMeetings()
    fun getMeetingsInNext7Days(): Flow<List<MeetingWithClient>> = meetingDao.getMeetingsInNext7Days()
    fun getMeetingsByDay(startOfDay: String, endOfDay: String): Flow<List<MeetingWithClient>> =
        meetingDao.getMeetingsByDay(startOfDay, endOfDay)

    fun getMeetingWithClientById(id: Long): Flow<MeetingWithClient?> =
        meetingDao.getMeetingWithClientById(id)
    suspend fun getMeetingWithClientByIdDirect(id: Long): MeetingWithClient? =
        meetingDao.getMeetingWithClientByIdDirect(id)
    suspend fun getMeetingsWithClientsByIds(ids: List<Long>): List<MeetingWithClient> =
        meetingDao.getMeetingsWithClientsByIds(ids)
    suspend fun getAllMeetingsWithClientsDirect(): List<MeetingWithClient> =
        meetingDao.getAllMeetingsWithClientsDirect()

    fun getUpcomingMeetingCount(): Flow<Int> = meetingDao.getUpcomingMeetingCount()

    suspend fun getMeetingById(id: Long): Meeting? = meetingDao.getMeetingById(id)
    suspend fun addMeeting(meeting: Meeting): Long = meetingDao.insertMeeting(meeting)
    suspend fun updateMeeting(meeting: Meeting) = meetingDao.updateMeeting(meeting)
    suspend fun deleteMeeting(meeting: Meeting) = meetingDao.deleteMeeting(meeting)
    suspend fun deleteMeetingById(id: Long) = meetingDao.deleteMeetingById(id)
    suspend fun updateMeetingStatus(id: Long, status: MeetingStatus) =
        meetingDao.updateMeetingStatus(id, status)
    suspend fun getAllMeetingsDirect(): List<Meeting> = meetingDao.getAllMeetingsDirect()
    suspend fun insertMeetings(meetings: List<Meeting>): List<Long> = meetingDao.insertMeetings(meetings)
}
