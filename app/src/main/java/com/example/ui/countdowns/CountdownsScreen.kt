package com.example.ui.countdowns

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Countdown
import com.example.ui.theme.BorderSlate700
import com.example.ui.theme.CountdownCategories
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepSlate900
import com.example.ui.theme.DeepSlate950
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.SurfaceSlate800
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TimeFormatUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownsScreen(
    viewModel: CountdownsViewModel,
    modifier: Modifier = Modifier
) {
    val countdowns by viewModel.displayedCountdowns.collectAsStateWithLifecycle()
    val allCountdowns by viewModel.allCountdowns.collectAsStateWithLifecycle()
    val activeFilter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isAddDialogOpen by viewModel.isAddDialogOpen.collectAsStateWithLifecycle()
    val editingCountdown by viewModel.editingCountdown.collectAsStateWithLifecycle()
    val countdownToDelete by viewModel.countdownToDelete.collectAsStateWithLifecycle()
    val highlightedId by viewModel.highlightedId.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    var currentTickerTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTickerTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(highlightedId, countdowns) {
        val target = highlightedId
        if (target != null && target != -1L) {
            val index = countdowns.indexOfFirst { it.id == target }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_countdowns"),
        containerColor = DeepSlate950,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = CyanPrimary,
                contentColor = DeepSlate950,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_countdown")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Countdown",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Countdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar
            CountdownHeaderSection(
                allCountdowns = allCountdowns,
                now = currentTickerTime
            )

            // Search and Sort Row
            SearchAndSortBar(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                sortOrder = sortOrder,
                onSortChange = { viewModel.setSortOrder(it) }
            )

            // Category Filter Pills
            CategoryFilterPillsRow(
                selectedCategory = selectedCategory,
                onCategorySelect = { viewModel.setSelectedCategory(it) }
            )

            // Time Status Filter Tabs (All, Upcoming, Past)
            FilterChipsRow(
                activeFilter = activeFilter,
                onFilterSelect = { viewModel.setFilter(it) },
                allCount = allCountdowns.size,
                upcomingCount = allCountdowns.count { it.targetTimeMillis > currentTickerTime },
                pastCount = allCountdowns.count { it.targetTimeMillis <= currentTickerTime }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Main List
            if (countdowns.isEmpty()) {
                EmptyCountdownsState(
                    isSearch = searchQuery.isNotBlank() || selectedCategory != "All",
                    onClearSearch = {
                        viewModel.setSearchQuery("")
                        viewModel.setSelectedCategory("All")
                    },
                    onAddNew = { viewModel.openAddDialog() }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(countdowns, key = { it.id }) { item ->
                        val isHighlighted = item.id == highlightedId

                        CountdownSwipeableCard(
                            countdown = item,
                            now = currentTickerTime,
                            isHighlighted = isHighlighted,
                            onTap = { viewModel.openEditDialog(item) },
                            onLongPress = { viewModel.requestDelete(item) },
                            onSwipeDelete = { viewModel.requestDelete(item) },
                            onDuplicate = { viewModel.duplicateCountdown(item) },
                            onSetAsWidget = { viewModel.setAsPrimaryWidgetCountdown(item) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (isAddDialogOpen) {
        AddEditCountdownDialog(
            countdown = editingCountdown,
            onDismiss = { viewModel.closeDialog() },
            onSave = { id, title, targetTime, createdTime, emoji, colorHex, notes, category, cardStyle, displayUnit ->
                viewModel.saveCountdown(
                    id = id,
                    title = title,
                    targetTimeMillis = targetTime,
                    createdTimeMillis = createdTime,
                    emoji = emoji,
                    colorHex = colorHex,
                    notes = notes,
                    category = category,
                    cardStyle = cardStyle,
                    displayUnit = displayUnit
                )
            }
        )
    }

    // Delete Confirmation Dialog
    countdownToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            containerColor = DeepSlate900,
            title = {
                Text(
                    text = "Delete Countdown?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${target.title}\"? Any home screen widgets displaying this event will reset.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelDelete() },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            modifier = Modifier.testTag("delete_confirmation_dialog")
        )
    }
}

@Composable
private fun CountdownHeaderSection(
    allCountdowns: List<Countdown>,
    now: Long
) {
    val upcomingCount = allCountdowns.count { it.targetTimeMillis > now }
    val pastCount = allCountdowns.count { it.targetTimeMillis <= now }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Phone Manager",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = CyanPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Text(
                text = "Countdowns",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceSlate800)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$upcomingCount active • $pastCount past",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun SearchAndSortBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search title, category, notes...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .testTag("search_countdowns_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceSlate800,
                unfocusedContainerColor = SurfaceSlate800,
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = BorderSlate700,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            IconButton(
                onClick = { sortMenuExpanded = true },
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceSlate800)
                    .testTag("button_sort_countdowns")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort Countdowns",
                    tint = CyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
                modifier = Modifier.background(DeepSlate900)
            ) {
                DropdownMenuItem(
                    text = { Text("Nearest date first", color = TextPrimary) },
                    onClick = {
                        onSortChange(SortOrder.NEAREST_DATE)
                        sortMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Furthest date first", color = TextPrimary) },
                    onClick = {
                        onSortChange(SortOrder.FURTHEST_DATE)
                        sortMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Alphabetical (A-Z)", color = TextPrimary) },
                    onClick = {
                        onSortChange(SortOrder.ALPHABETICAL)
                        sortMenuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterPillsRow(
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    val categoriesWithAll = listOf("All") + CountdownCategories

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categoriesWithAll.forEach { cat ->
            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) CyanPrimary else SurfaceSlate800)
                    .clickable { onCategorySelect(cat) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cat,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) DeepSlate950 else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    activeFilter: CountdownFilter,
    onFilterSelect: (CountdownFilter) -> Unit,
    allCount: Int,
    upcomingCount: Int,
    pastCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = activeFilter == CountdownFilter.ALL,
            onClick = { onFilterSelect(CountdownFilter.ALL) },
            label = { Text("All ($allCount)", fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                selectedLabelColor = CyanPrimary,
                containerColor = SurfaceSlate800,
                labelColor = TextSecondary
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = if (activeFilter == CountdownFilter.ALL) CyanPrimary else BorderSlate700,
                enabled = true,
                selected = activeFilter == CountdownFilter.ALL
            ),
            modifier = Modifier.testTag("filter_chip_all")
        )

        FilterChip(
            selected = activeFilter == CountdownFilter.UPCOMING,
            onClick = { onFilterSelect(CountdownFilter.UPCOMING) },
            label = { Text("Upcoming ($upcomingCount)", fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                selectedLabelColor = CyanPrimary,
                containerColor = SurfaceSlate800,
                labelColor = TextSecondary
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = if (activeFilter == CountdownFilter.UPCOMING) CyanPrimary else BorderSlate700,
                enabled = true,
                selected = activeFilter == CountdownFilter.UPCOMING
            ),
            modifier = Modifier.testTag("filter_chip_upcoming")
        )

        FilterChip(
            selected = activeFilter == CountdownFilter.PAST,
            onClick = { onFilterSelect(CountdownFilter.PAST) },
            label = { Text("Past ($pastCount)", fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                selectedLabelColor = CyanPrimary,
                containerColor = SurfaceSlate800,
                labelColor = TextSecondary
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = if (activeFilter == CountdownFilter.PAST) CyanPrimary else BorderSlate700,
                enabled = true,
                selected = activeFilter == CountdownFilter.PAST
            ),
            modifier = Modifier.testTag("filter_chip_past")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CountdownSwipeableCard(
    countdown: Countdown,
    now: Long,
    isHighlighted: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onSetAsWidget: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSwipeDelete()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(RoseDanger.copy(alpha = 0.85f))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }
    ) {
        CountdownCardContent(
            countdown = countdown,
            now = now,
            isHighlighted = isHighlighted,
            onTap = onTap,
            onLongPress = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress()
            },
            onDuplicate = onDuplicate,
            onSetAsWidget = onSetAsWidget
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CountdownCardContent(
    countdown: Countdown,
    now: Long,
    isHighlighted: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDuplicate: () -> Unit,
    onSetAsWidget: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val timeRemaining = remember(countdown.targetTimeMillis, countdown.createdTimeMillis, countdown.displayUnit, now) {
        TimeFormatUtils.computeRemaining(
            targetTimeMillis = countdown.targetTimeMillis,
            createdTimeMillis = countdown.createdTimeMillis,
            displayUnit = countdown.displayUnit,
            now = now
        )
    }

    val accentColor = remember(countdown.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(countdown.colorHex))
        } catch (e: Exception) {
            CyanPrimary
        }
    }

    // Dynamic background based on cardStyle
    val cardBackground = when {
        timeRemaining.isPast -> Color(0xFF161F30)
        countdown.cardStyle == "tinted" -> accentColor.copy(alpha = 0.12f)
        countdown.cardStyle == "minimal" -> DeepSlate900
        else -> SurfaceSlate800
    }

    val cardBorder = when {
        isHighlighted -> CyanPrimary
        countdown.cardStyle == "minimal" -> accentColor.copy(alpha = 0.6f)
        else -> BorderSlate700.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isHighlighted || countdown.cardStyle == "minimal") 1.8.dp else 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
            .testTag("countdown_card_${countdown.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Emoji, Title, Category Badge, and More Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (timeRemaining.isPast) Color(0xFF334155).copy(alpha = 0.5f)
                            else accentColor.copy(alpha = 0.22f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdown.emoji,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = countdown.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (timeRemaining.isPast) TextSecondary else TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (timeRemaining.isPast) Color(0xFF334155)
                                    else accentColor.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = countdown.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = if (timeRemaining.isPast) TextMuted else accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = TimeFormatUtils.formatDate(countdown.targetTimeMillis),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Time Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (timeRemaining.isPast) Color(0xFF1E293B)
                            else accentColor.copy(alpha = 0.22f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = timeRemaining.customDisplaySummary,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (timeRemaining.isPast) TextMuted else accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Options Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(DeepSlate900)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Display on Widget", color = CyanPrimary) },
                            leadingIcon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = CyanPrimary) },
                            onClick = {
                                menuExpanded = false
                                onSetAsWidget()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Customize / Edit", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = CyanPrimary) },
                            onClick = {
                                menuExpanded = false
                                onTap()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanPrimary) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = RoseDanger) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RoseDanger) },
                            onClick = {
                                menuExpanded = false
                                onLongPress()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time breakdown ticker
            if (!timeRemaining.isPast) {
                when (countdown.displayUnit) {
                    "days" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL DAYS REMAINING",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${timeRemaining.totalDays} DAYS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = accentColor)
                            )
                        }
                    }
                    "hours" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL HOURS REMAINING",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${timeRemaining.totalHours} HOURS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = accentColor)
                            )
                        }
                    }
                    "weeks" -> {
                        val weeks = timeRemaining.totalDays / 7
                        val days = timeRemaining.totalDays % 7
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "WEEKS & DAYS REMAINING",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${weeks}w ${days}d",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = accentColor)
                            )
                        }
                    }
                    else -> {
                        // Standard full breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeUnitBox(value = timeRemaining.totalDays.toString(), label = "DAYS", accent = accentColor, modifier = Modifier.weight(1f))
                            TimeUnitBox(value = String.format("%02d", timeRemaining.hours), label = "HOURS", accent = accentColor, modifier = Modifier.weight(1f))
                            TimeUnitBox(value = String.format("%02d", timeRemaining.minutes), label = "MINS", accent = accentColor, modifier = Modifier.weight(1f))
                            TimeUnitBox(value = String.format("%02d", timeRemaining.seconds), label = "SECS", accent = accentColor, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { timeRemaining.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = Color(0xFF0F172A),
                    strokeCap = StrokeCap.Round
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Completed milestone",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                    Text(
                        text = timeRemaining.badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Optional Notes
            if (countdown.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = countdown.notes,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (timeRemaining.isPast) TextMuted else TextSecondary,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TimeUnitBox(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun EmptyCountdownsState(
    isSearch: Boolean,
    onClearSearch: () -> Unit,
    onAddNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SurfaceSlate800),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSearch) "No Matching Countdowns" else "No Countdowns Yet",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isSearch) {
                "Try searching for another keyword or select 'All' categories."
            } else {
                "Create and customize your countdowns with colors, categories, styles, and live home screen widgets."
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isSearch) {
            Button(
                onClick = onClearSearch,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceSlate800)
            ) {
                Text("Clear Filters", color = CyanPrimary)
            }
        } else {
            Button(
                onClick = onAddNew,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Add Your First Countdown", color = DeepSlate950, fontWeight = FontWeight.Bold)
            }
        }
    }
}
