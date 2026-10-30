package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VipContactRepository(private val vipContactDao: VipContactDao) {

    val allContacts: Flow<List<VipContact>> = vipContactDao.getAllContactsFlow()
    val enabledContacts: Flow<List<VipContact>> = vipContactDao.getEnabledContactsFlow()

    suspend fun getEnabledContactsSync(): List<VipContact> = withContext(Dispatchers.IO) {
        vipContactDao.getEnabledContactsSync()
    }

    suspend fun getContactById(id: Long): VipContact? = withContext(Dispatchers.IO) {
        vipContactDao.getContactById(id)
    }

    suspend fun insertContact(contact: VipContact): Long = withContext(Dispatchers.IO) {
        vipContactDao.insertContact(contact)
    }

    suspend fun insertContacts(contacts: List<VipContact>): List<Long> = withContext(Dispatchers.IO) {
        vipContactDao.insertContacts(contacts)
    }

    suspend fun updateContact(contact: VipContact) = withContext(Dispatchers.IO) {
        vipContactDao.updateContact(contact)
    }

    suspend fun updateEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        vipContactDao.updateEnabled(id, enabled)
    }

    suspend fun deleteContact(contact: VipContact) = withContext(Dispatchers.IO) {
        vipContactDao.deleteContact(contact)
    }

    suspend fun deleteContactById(id: Long) = withContext(Dispatchers.IO) {
        vipContactDao.deleteContactById(id)
    }

    suspend fun deleteAllContacts() = withContext(Dispatchers.IO) {
        vipContactDao.deleteAllContacts()
    }

    suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        vipContactDao.getCount()
    }
}
