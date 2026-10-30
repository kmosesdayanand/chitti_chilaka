package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VipContactDao {

    @Query("SELECT * FROM vip_contacts ORDER BY contactName COLLATE NOCASE ASC, createdAt DESC")
    fun getAllContactsFlow(): Flow<List<VipContact>>

    @Query("SELECT * FROM vip_contacts WHERE enabled = 1")
    fun getEnabledContactsFlow(): Flow<List<VipContact>>

    @Query("SELECT * FROM vip_contacts WHERE enabled = 1")
    fun getEnabledContactsSync(): List<VipContact>

    @Query("SELECT * FROM vip_contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Long): VipContact?

    @Query("SELECT * FROM vip_contacts WHERE id = :id LIMIT 1")
    fun getContactByIdSync(id: Long): VipContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: VipContact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<VipContact>): List<Long>

    @Update
    suspend fun updateContact(contact: VipContact)

    @Query("UPDATE vip_contacts SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, enabled: Boolean)

    @Delete
    suspend fun deleteContact(contact: VipContact)

    @Query("DELETE FROM vip_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("DELETE FROM vip_contacts")
    suspend fun deleteAllContacts()

    @Query("SELECT COUNT(*) FROM vip_contacts")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM vip_contacts")
    fun getCountSync(): Int
}
