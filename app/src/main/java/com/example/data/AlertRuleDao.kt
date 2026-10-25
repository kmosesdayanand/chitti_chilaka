package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertRuleDao {

    @Query("SELECT * FROM alert_rules ORDER BY level ASC")
    fun getAllRules(): Flow<List<AlertRule>>

    @Query("SELECT * FROM alert_rules ORDER BY level ASC")
    suspend fun getAllRulesSync(): List<AlertRule>

    @Query("SELECT * FROM alert_rules WHERE enabled = 1 ORDER BY level ASC")
    suspend fun getEnabledRulesSync(): List<AlertRule>

    @Query("SELECT * FROM alert_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleByIdSync(id: Long): AlertRule?

    @Query("SELECT COUNT(*) FROM alert_rules")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AlertRule): Long

    @Update
    suspend fun updateRule(rule: AlertRule)

    @Delete
    suspend fun deleteRule(rule: AlertRule)

    @Query("DELETE FROM alert_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("UPDATE alert_rules SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE alert_rules SET lastTriggered = :lastTriggered WHERE id = :id")
    suspend fun updateTriggeredState(id: Long, lastTriggered: Boolean)

    @Query("UPDATE alert_rules SET soundType = :newSoundType WHERE direction = :direction AND soundType != 'CUSTOM'")
    suspend fun updateSoundTypeForDirection(direction: String, newSoundType: String)
}
