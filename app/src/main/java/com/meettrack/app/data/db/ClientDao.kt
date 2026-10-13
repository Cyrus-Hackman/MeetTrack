package com.meettrack.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.ClientWithMeetings
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    @Query("SELECT * FROM clients ORDER BY fullName ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    fun getClientById(id: Long): Flow<Client?>

    @Query("""
        SELECT * FROM clients 
        WHERE fullName LIKE '%' || :query || '%'
        OR phoneNumber LIKE '%' || :query || '%'
        OR company LIKE '%' || :query || '%'
        OR address LIKE '%' || :query || '%'
        ORDER BY fullName ASC
    """)
    fun searchClients(query: String): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY fullName ASC")
    fun getClientsWithLocation(): Flow<List<Client>>

    @Query("SELECT * FROM clients ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentClients(limit: Int = 5): Flow<List<Client>>

    @Query("SELECT COUNT(*) FROM clients")
    fun getClientCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM clients WHERE id = :clientId")
    fun getClientWithMeetings(clientId: Long): Flow<ClientWithMeetings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteClientById(id: Long)

    @Query("SELECT * FROM clients ORDER BY fullName ASC")
    suspend fun getAllClientsDirect(): List<Client>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<Client>): List<Long>
}
