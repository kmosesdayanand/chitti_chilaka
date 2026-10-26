package com.example.ui.alerts

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlertRule
import com.example.data.AlertRuleRepository
import com.example.data.AppDatabase
import com.example.service.BatteryAlertService
import com.example.util.AudioAlertPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlertRuleRepository

    val allRules: StateFlow<List<AlertRule>>

    private val _currentBatteryPct = MutableStateFlow(100)
    val currentBatteryPct: StateFlow<Int> = _currentBatteryPct.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _editingRule = MutableStateFlow<AlertRule?>(null)
    val editingRule: StateFlow<AlertRule?> = _editingRule.asStateFlow()

    private val _isAddDialogOpen = MutableStateFlow(false)
    val isAddDialogOpen: StateFlow<Boolean> = _isAddDialogOpen.asStateFlow()

    private val _ruleToDelete = MutableStateFlow<AlertRule?>(null)
    val ruleToDelete: StateFlow<AlertRule?> = _ruleToDelete.asStateFlow()

    private val _playingRuleId = MutableStateFlow<Long?>(null)
    val playingRuleId: StateFlow<Long?> = _playingRuleId.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                updateBatteryState(intent)
            }
        }
    }

    init {
        val db = AppDatabase.getInstance(application)
        repository = AlertRuleRepository(db.alertRuleDao())

        allRules = repository.allRules.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Read initial sticky battery intent
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = ContextCompat.registerReceiver(
            application,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        if (initialIntent != null) {
            updateBatteryState(initialIntent)
        }

        viewModelScope.launch {
            BatteryAlertService.syncWithDatabase(application)
        }
    }

    private fun updateBatteryState(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) {
            _currentBatteryPct.value = (level * 100) / scale
        }
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun openAddDialog() {
        _editingRule.value = null
        _isAddDialogOpen.value = true
    }

    fun openEditDialog(rule: AlertRule) {
        _editingRule.value = rule
        _isAddDialogOpen.value = true
    }

    fun closeDialog() {
        _editingRule.value = null
        _isAddDialogOpen.value = false
        stopTestSound()
    }

    fun requestDelete(rule: AlertRule) {
        _ruleToDelete.value = rule
    }

    fun cancelDelete() {
        _ruleToDelete.value = null
    }

    fun confirmDelete() {
        val rule = _ruleToDelete.value ?: return
        viewModelScope.launch {
            repository.delete(rule)
            _ruleToDelete.value = null
            BatteryAlertService.syncWithDatabase(getApplication())
        }
    }

    fun toggleRule(rule: AlertRule, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(rule.id, enabled)
            BatteryAlertService.syncWithDatabase(getApplication())
        }
    }

    fun saveRule(
        id: Long = 0,
        level: Int,
        direction: String,
        soundType: String,
        soundUri: String?,
        volume: Float,
        enabled: Boolean = true,
        startMs: Int = 0,
        endMs: Int = 0
    ) {
        viewModelScope.launch {
            val rule = AlertRule(
                id = id,
                level = level.coerceIn(1, 100),
                direction = direction,
                soundType = soundType,
                soundUri = soundUri,
                volume = volume.coerceIn(0.1f, 1.0f),
                enabled = enabled,
                lastTriggered = false,
                startMs = startMs,
                endMs = endMs
            )
            if (id == 0L) {
                repository.insert(rule)
            } else {
                repository.update(rule)
            }
            closeDialog()
            BatteryAlertService.syncWithDatabase(getApplication())
        }
    }

    fun testSound(
        ruleId: Long,
        soundType: String,
        soundUri: String?,
        volume: Float,
        startMs: Int = 0,
        endMs: Int = 0
    ) {
        if (_playingRuleId.value == ruleId) {
            stopTestSound()
            return
        }

        _playingRuleId.value = ruleId
        AudioAlertPlayer.playSound(
            context = getApplication(),
            soundType = soundType,
            soundUriString = soundUri,
            volume = volume,
            startMs = startMs,
            endMs = endMs,
            onStopCallback = {
                _playingRuleId.value = null
            }
        )
    }

    fun stopTestSound() {
        AudioAlertPlayer.stop()
        _playingRuleId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopTestSound()
    }
}
