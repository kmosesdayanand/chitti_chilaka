package com.example.data

import kotlinx.coroutines.flow.Flow

class CountdownRepository(private val dao: CountdownDao) {

    val allCountdowns: Flow<List<Countdown>> = dao.getAllCountdowns()

    suspend fun getAllCountdownsSync(): List<Countdown> = dao.getAllCountdownsSync()

    fun getCountdownById(id: Long): Flow<Countdown?> = dao.getCountdownById(id)

    suspend fun getCountdownByIdSync(id: Long): Countdown? = dao.getCountdownByIdSync(id)

    suspend fun getUpcomingCountdownsSync(limit: Int): List<Countdown> =
        dao.getUpcomingCountdownsSync(System.currentTimeMillis(), limit)

    suspend fun insert(countdown: Countdown): Long = dao.insertCountdown(countdown)

    suspend fun update(countdown: Countdown) = dao.updateCountdown(countdown)

    suspend fun delete(countdown: Countdown) = dao.deleteCountdown(countdown)

    suspend fun deleteById(id: Long) = dao.deleteCountdownById(id)
}
