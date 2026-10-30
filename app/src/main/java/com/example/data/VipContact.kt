package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.PhoneNumberMatcher

@Entity(tableName = "vip_contacts")
data class VipContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactName: String,
    val phoneNumber: String, // Raw phone number as entered or picked
    val normalizedNumber: String = PhoneNumberMatcher.normalize(phoneNumber),
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val photoUri: String? = null
)
