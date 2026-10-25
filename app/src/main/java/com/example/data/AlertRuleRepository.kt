package com.example.data

import kotlinx.coroutines.flow.Flow

class AlertRuleRepository(private val dao: AlertRuleDao) {

    val allRules: Flow<List<AlertRule>> = dao.getAllRules()

    suspend fun getAllRulesSync(): List<AlertRule> = dao.getAllRulesSync()

    suspend fun getEnabledRulesSync(): List<AlertRule> = dao.getEnabledRulesSync()

    suspend fun insert(rule: AlertRule): Long = dao.insertRule(rule)

    suspend fun update(rule: AlertRule) = dao.updateRule(rule)

    suspend fun delete(rule: AlertRule) = dao.deleteRule(rule)

    suspend fun deleteById(id: Long) = dao.deleteRuleById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.updateEnabled(id, enabled)

    suspend fun setTriggered(id: Long, triggered: Boolean) = dao.updateTriggeredState(id, triggered)
}
