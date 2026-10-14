package com.meettrack.app.data.repository

import com.meettrack.app.data.db.ClientDao
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.ClientWithMeetings
import com.meettrack.app.data.model.parseReminderIntervals
import com.meettrack.app.notifications.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(
    private val clientDao: ClientDao,
    private val meetingRepository: MeetingRepository,
    private val notificationScheduler: NotificationScheduler
) {
    fun getAllClients(): Flow<List<Client>> = clientDao.getAllClients()
    fun searchClients(query: String): Flow<List<Client>> = clientDao.searchClients(query)
    fun getClientsWithLocation(): Flow<List<Client>> = clientDao.getClientsWithLocation()
    fun getRecentClients(limit: Int = 5): Flow<List<Client>> = clientDao.getRecentClients(limit)
    fun getClientCount(): Flow<Int> = clientDao.getClientCount()
    fun getClientById(id: Long): Flow<Client?> = clientDao.getClientById(id)
    suspend fun getClientByIdDirect(id: Long): Client? = clientDao.getClientByIdDirect(id)
    fun getClientWithMeetings(clientId: Long): Flow<ClientWithMeetings?> =
        clientDao.getClientWithMeetings(clientId)

    suspend fun addClient(client: Client): Long = clientDao.insertClient(client)
    suspend fun updateClient(client: Client) = clientDao.updateClient(client)
    suspend fun deleteClientAndCancelReminders(id: Long) {
        meetingRepository.getMeetingsForClientDirect(id).forEach { meeting ->
            notificationScheduler.cancelReminders(
                meeting.id,
                parseReminderIntervals(meeting.reminderIntervals)
            )
        }
        clientDao.deleteClientById(id)
    }
    suspend fun getAllClientsDirect(): List<Client> = clientDao.getAllClientsDirect()
    suspend fun insertClients(clients: List<Client>): List<Long> = clientDao.insertClients(clients)
}
