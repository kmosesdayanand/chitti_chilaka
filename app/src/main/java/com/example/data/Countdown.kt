package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "countdowns")
data class Countdown(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetTimeMillis: Long,
    val createdTimeMillis: Long = System.currentTimeMillis(),
    val emoji: String = "⏳",
    val colorHex: String = "#38BDF8",
    val notes: String = "",
    val category: String = "General",
    val cardStyle: String = "modern", // "modern", "tinted", "minimal"
    val displayUnit: String = "all" // "all", "days", "hours", "weeks"
)
