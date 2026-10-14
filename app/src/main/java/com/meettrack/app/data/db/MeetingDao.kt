package com.meettrack.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.Update
import com.meettrack.app.data.model.Meeting
import com.meettrack.app.data.model.MeetingStatus
import com.meettrack.app.data.model.MeetingWithClient
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    @Transaction
    @Query("SELECT * FROM meetings ORDER BY dateTime ASC")
    fun getAllMeetingsWithClient(): Flow<List<MeetingWithClient>>

    @Transaction
    @Query("SELECT * FROM meetings WHERE clientId = :clientId ORDER BY dateTime DESC")
    fun getMeetingsForClient(clientId: Long): Flow<List<MeetingWithClient>>

    @Query("SELECT id FROM meetings WHERE clientId = :clientId")
    suspend fun getMeetingIdsForClient(clientId: Long): List<Long>

    @Transaction
    @Query("""
        SELECT * FROM meetings 
        WHERE date(datetime(dateTime)) = date('now', 'localtime')
        ORDER BY dateTime ASC
    """)
    fun getTodaysMeetings(): Flow<List<MeetingWithClient>>

    @Transaction
    @Query("""
        SELECT * FROM meetings 
        WHERE status IN ('UPCOMING', 'RESCHEDULED')
        AND dateTime > strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')
        ORDER BY dateTime ASC
    """)
    fun getUpcomingMeetings(): Flow<List<MeetingWithClient>>

    @Transaction
    @Query("""
        SELECT * FROM meetings 
        WHERE status IN ('UPCOMING', 'RESCHEDULED')
        AND dateTime < strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')
        ORDER BY dateTime DESC
    """)
    fun getOverdueMeetings(): Flow<List<MeetingWithClient>>

    @Transaction
    @Query("""
        SELECT * FROM meetings
        WHERE status IN ('UPCOMING', 'RESCHEDULED')
        AND dateTime BETWEEN strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')
        AND strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '+7 days')
        ORDER BY dateTime ASC
    """)
    fun getMeetingsInNext7Days(): Flow<List<MeetingWithClient>>

    @Transaction
    @Query("""
        SELECT * FROM meetings
        WHERE status IN ('UPCOMING', 'RESCHEDULED')
        AND dateTime BETWEEN :startOfDay AND :endOfDay
        ORDER BY dateTime ASC
    """)
    fun getMeetingsByDay(startOfDay: String, endOfDay: String): Flow<List<MeetingWithClient>>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getMeetingById(id: Long): Meeting?

    @Transaction
    @Query("SELECT * FROM meetings WHERE id = :id")
    fun getMeetingWithClientById(id: Long): Flow<MeetingWithClient?>

    @Transaction
    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getMeetingWithClientByIdDirect(id: Long): MeetingWithClient?

    @Transaction
    @Query("SELECT * FROM meetings WHERE id IN (:ids)")
    suspend fun getMeetingsWithClientsByIds(ids: List<Long>): List<MeetingWithClient>

    @Query("SELECT COUNT(*) FROM meetings WHERE status IN ('UPCOMING', 'RESCHEDULED')")
    fun getUpcomingMeetingCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM meetings ORDER BY dateTime ASC")
    suspend fun getAllMeetingsWithClientsDirect(): List<MeetingWithClient>

    @Upsert
    suspend fun insertMeeting(meeting: Meeting): Long

    @Update
    suspend fun updateMeeting(meeting: Meeting)

    @Delete
    suspend fun deleteMeeting(meeting: Meeting)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteMeetingById(id: Long)

    @Query("UPDATE meetings SET status = :status WHERE id = :id")
    suspend fun updateMeetingStatus(id: Long, status: MeetingStatus)

    @Query("SELECT * FROM meetings ORDER BY dateTime ASC")
    suspend fun getAllMeetingsDirect(): List<Meeting>

    @Upsert
    suspend fun insertMeetings(meetings: List<Meeting>): List<Long>
}
