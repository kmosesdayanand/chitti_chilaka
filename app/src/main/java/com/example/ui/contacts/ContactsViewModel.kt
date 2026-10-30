package com.example.ui.contacts

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VipContact
import com.example.data.VipContactRepository
import com.example.data.VipPreferences
import com.example.data.VipSettings
import com.example.service.VipRingManager
import com.example.util.PhoneNumberMatcher
import com.example.util.VipPermissionChecker
import com.example.util.VipPermissionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VipContactRepository
    private val vipPreferences: VipPreferences = VipPreferences.getInstance(application)

    val allContacts: StateFlow<List<VipContact>>
    val vipSettings: StateFlow<VipSettings>

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _permissionsChecklist = MutableStateFlow<List<VipPermissionItem>>(emptyList())
    val permissionsChecklist: StateFlow<List<VipPermissionItem>> = _permissionsChecklist.asStateFlow()

    private val _hasAllEssentialPermissions = MutableStateFlow(false)
    val hasAllEssentialPermissions: StateFlow<Boolean> = _hasAllEssentialPermissions.asStateFlow()

    private val _testRingSecondsLeft = MutableStateFlow<Int?>(null)
    val testRingSecondsLeft: StateFlow<Int?> = _testRingSecondsLeft.asStateFlow()

    private val _lastDeletedContact = MutableStateFlow<VipContact?>(null)
    val lastDeletedContact: StateFlow<VipContact?> = _lastDeletedContact.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = VipContactRepository(db.vipContactDao())

        allContacts = repository.allContacts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        vipSettings = vipPreferences.vipSettingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VipSettings()
        )

        refreshPermissions()
    }

    val filteredContacts: StateFlow<List<VipContact>> = combine(
        allContacts,
        _searchQuery
    ) { contacts, query ->
        if (query.isBlank()) {
            contacts
        } else {
            val q = query.trim().lowercase()
            contacts.filter { contact ->
                contact.contactName.lowercase().contains(q) ||
                contact.phoneNumber.contains(q) ||
                contact.normalizedNumber.contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        _permissionsChecklist.value = VipPermissionChecker.getChecklist(context)
        _hasAllEssentialPermissions.value = VipPermissionChecker.hasAllEssentialPermissions(context)
    }

    fun toggleContactEnabled(contact: VipContact) {
        viewModelScope.launch {
            repository.updateEnabled(contact.id, !contact.enabled)
        }
    }

    fun deleteContact(contact: VipContact) {
        viewModelScope.launch {
            _lastDeletedContact.value = contact
            repository.deleteContact(contact)
        }
    }

    fun undoDelete() {
        val contact = _lastDeletedContact.value ?: return
        viewModelScope.launch {
            repository.insertContact(contact)
            _lastDeletedContact.value = null
        }
    }

    fun clearUndo() {
        _lastDeletedContact.value = null
    }

    fun deleteAllContacts() {
        viewModelScope.launch {
            repository.deleteAllContacts()
        }
    }

    /**
     * Resolves selected contact from ContactsContract picker.
     * If the contact has multiple numbers, each number is saved as a separate VIP entry.
     */
    fun importContactFromUri(context: Context, uri: Uri, onResult: (countAdded: Int) -> Unit) {
        viewModelScope.launch {
            var addedCount = 0
            try {
                val cr = context.contentResolver
                var contactName: String? = null
                var photoUri: String? = null
                val numbers = mutableListOf<String>()

                // Check if URI is already a Phone record or a Contact record
                val isPhoneUri = uri.toString().contains("data") || uri.toString().contains("phones")

                if (isPhoneUri) {
                    cr.query(
                        uri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                        ),
                        null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            contactName = cursor.getString(0)
                            cursor.getString(1)?.let { numbers.add(it) }
                            photoUri = cursor.getString(2)
                        }
                    }
                } else {
                    var contactId: Long = -1L
                    cr.query(
                        uri,
                        arrayOf(
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts.PHOTO_URI
                        ),
                        null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            contactId = cursor.getLong(0)
                            contactName = cursor.getString(1)
                            photoUri = cursor.getString(2)
                        }
                    }

                    if (contactId != -1L) {
                        cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId.toString()),
                            null
                        )?.use { phoneCursor ->
                            val numberCol = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            while (phoneCursor.moveToNext()) {
                                val num = phoneCursor.getString(numberCol)
                                if (!num.isNullOrBlank() && !numbers.contains(num)) {
                                    numbers.add(num)
                                }
                            }
                        }
                    }
                }

                val finalName = contactName ?: "Unknown Contact"
                if (numbers.isNotEmpty()) {
                    val entries = numbers.map { rawNumber ->
                        VipContact(
                            contactName = finalName,
                            phoneNumber = rawNumber,
                            normalizedNumber = PhoneNumberMatcher.normalize(rawNumber),
                            enabled = true,
                            photoUri = photoUri
                        )
                    }
                    repository.insertContacts(entries)
                    addedCount = entries.size
                }
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Error importing contact from picker", e)
            }
            onResult(addedCount)
        }
    }

    fun addManualContact(name: String, phoneNumber: String, onResult: (Boolean) -> Unit) {
        if (name.isBlank() || phoneNumber.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                val contact = VipContact(
                    contactName = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    normalizedNumber = PhoneNumberMatcher.normalize(phoneNumber),
                    enabled = true
                )
                repository.insertContact(contact)
                onResult(true)
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Failed to add manual contact", e)
                onResult(false)
            }
        }
    }

    // Settings actions
    fun setVipRingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            vipPreferences.setVipRingEnabled(enabled)
        }
    }

    fun setRingVolumePercent(volume: Float) {
        viewModelScope.launch {
            vipPreferences.setRingVolumePercent(volume)
        }
    }

    fun setOverrideDnd(enabled: Boolean) {
        viewModelScope.launch {
            vipPreferences.setOverrideDnd(enabled)
        }
    }

    fun setAlsoVibrate(enabled: Boolean) {
        viewModelScope.launch {
            vipPreferences.setAlsoVibrate(enabled)
        }
    }

    fun setRingOnSilent(enabled: Boolean) {
        viewModelScope.launch {
            vipPreferences.setRingOnSilent(enabled)
        }
    }

    fun startTestRing() {
        val context = getApplication<Application>()
        VipRingManager.startTestRing(
            context = context,
            onTick = { seconds ->
                _testRingSecondsLeft.value = seconds
            },
            onFinished = {
                _testRingSecondsLeft.value = null
            }
        )
    }

    fun stopTestRing() {
        val context = getApplication<Application>()
        VipRingManager.stopTestRing(context) {
            _testRingSecondsLeft.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTestRing()
    }
}
