package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {

    @Query("SELECT * FROM countdowns ORDER BY targetTimeMillis ASC")
    fun getAllCountdowns(): Flow<List<Countdown>>

    @Query("SELECT * FROM countdowns ORDER BY targetTimeMillis ASC")
    suspend fun getAllCountdownsSync(): List<Countdown>

    @Query("SELECT * FROM countdowns WHERE id = :id LIMIT 1")
    fun getCountdownById(id: Long): Flow<Countdown?>

    @Query("SELECT * FROM countdowns WHERE id = :id LIMIT 1")
    suspend fun getCountdownByIdSync(id: Long): Countdown?

    @Query("SELECT * FROM countdowns WHERE targetTimeMillis >= :now ORDER BY targetTimeMillis ASC LIMIT :limit")
    suspend fun getUpcomingCountdownsSync(now: Long, limit: Int): List<Countdown>

    @Query("SELECT COUNT(*) FROM countdowns")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountdown(countdown: Countdown): Long

    @Update
    suspend fun updateCountdown(countdown: Countdown)

    @Delete
    suspend fun deleteCountdown(countdown: Countdown)

    @Query("DELETE FROM countdowns WHERE id = :id")
    suspend fun deleteCountdownById(id: Long)
}
