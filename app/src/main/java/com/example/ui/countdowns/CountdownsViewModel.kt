package com.example.ui.countdowns

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Countdown
import com.example.data.CountdownRepository
import com.example.widget.WidgetUpdater
import com.example.worker.CountdownWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CountdownFilter {
    ALL,
    UPCOMING,
    PAST
}

enum class SortOrder {
    NEAREST_DATE,
    FURTHEST_DATE,
    ALPHABETICAL
}

class CountdownsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CountdownRepository

    val allCountdowns: StateFlow<List<Countdown>>

    private val _filter = MutableStateFlow(CountdownFilter.ALL)
    val filter: StateFlow<CountdownFilter> = _filter.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEAREST_DATE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _editingCountdown = MutableStateFlow<Countdown?>(null)
    val editingCountdown: StateFlow<Countdown?> = _editingCountdown.asStateFlow()

    private val _isAddDialogOpen = MutableStateFlow(false)
    val isAddDialogOpen: StateFlow<Boolean> = _isAddDialogOpen.asStateFlow()

    private val _countdownToDelete = MutableStateFlow<Countdown?>(null)
    val countdownToDelete: StateFlow<Countdown?> = _countdownToDelete.asStateFlow()

    private val _highlightedId = MutableStateFlow<Long?>(null)
    val highlightedId: StateFlow<Long?> = _highlightedId.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = CountdownRepository(db.countdownDao())

        allCountdowns = repository.allCountdowns.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            AppDatabase.seedSamples(db)
            try {
                CountdownWorker.schedulePeriodicUpdates(application)
                WidgetUpdater.updateAllWidgets(application)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val displayedCountdowns: StateFlow<List<Countdown>> = combine(
        allCountdowns,
        _filter,
        _selectedCategory,
        _searchQuery,
        _sortOrder
    ) { list, filter, category, query, sort ->
        val now = System.currentTimeMillis()

        var filtered = when (filter) {
            CountdownFilter.ALL -> list
            CountdownFilter.UPCOMING -> list.filter { it.targetTimeMillis > now }
            CountdownFilter.PAST -> list.filter { it.targetTimeMillis <= now }
        }

        if (category != "All") {
            filtered = filtered.filter { it.category.equals(category, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.emoji.contains(query)
            }
        }

        when (sort) {
            SortOrder.NEAREST_DATE -> filtered.sortedBy { it.targetTimeMillis }
            SortOrder.FURTHEST_DATE -> filtered.sortedByDescending { it.targetTimeMillis }
            SortOrder.ALPHABETICAL -> filtered.sortedBy { it.title.lowercase() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: CountdownFilter) {
        _filter.value = filter
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun openAddDialog() {
        _editingCountdown.value = null
        _isAddDialogOpen.value = true
    }

    fun openEditDialog(countdown: Countdown) {
        _editingCountdown.value = countdown
        _isAddDialogOpen.value = true
    }

    fun closeDialog() {
        _editingCountdown.value = null
        _isAddDialogOpen.value = false
    }

    fun requestDelete(countdown: Countdown) {
        _countdownToDelete.value = countdown
    }

    fun cancelDelete() {
        _countdownToDelete.value = null
    }

    fun confirmDelete() {
        val target = _countdownToDelete.value ?: return
        viewModelScope.launch {
            repository.delete(target)
            _countdownToDelete.value = null
            WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun duplicateCountdown(countdown: Countdown) {
        viewModelScope.launch {
            val duplicate = countdown.copy(
                id = 0,
                title = "${countdown.title} (Copy)",
                createdTimeMillis = System.currentTimeMillis()
            )
            repository.insert(duplicate)
            WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun setAsPrimaryWidgetCountdown(countdown: Countdown) {
        viewModelScope.launch {
            WidgetUpdater.setPrimaryCountdownAndRefreshAll(getApplication(), countdown.id)
        }
    }

    fun saveCountdown(
        id: Long = 0,
        title: String,
        targetTimeMillis: Long,
        createdTimeMillis: Long,
        emoji: String,
        colorHex: String,
        notes: String,
        category: String = "General",
        cardStyle: String = "modern",
        displayUnit: String = "all"
    ) {
        viewModelScope.launch {
            val countdown = Countdown(
                id = id,
                title = title.trim(),
                targetTimeMillis = targetTimeMillis,
                createdTimeMillis = if (id == 0L) System.currentTimeMillis() else createdTimeMillis,
                emoji = emoji.trim().ifEmpty { "⏳" },
                colorHex = colorHex,
                notes = notes.trim(),
                category = category,
                cardStyle = cardStyle,
                displayUnit = displayUnit
            )

            if (id == 0L) {
                repository.insert(countdown)
            } else {
                repository.update(countdown)
            }

            closeDialog()
            WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun setHighlightedId(id: Long?) {
        _highlightedId.value = id
    }
}
