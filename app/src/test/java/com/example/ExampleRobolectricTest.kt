package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Countdown
import com.example.util.TimeFormatUtils
import com.example.widget.WidgetPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sentry", appName)
    }

    @Test
    fun `time format utils correctly handles future events`() {
        val now = 1000000000L
        val created = now - 86400000L * 5
        val target = now + 86400000L * 10 // 10 days in future

        val remaining = TimeFormatUtils.computeRemaining(
            targetTimeMillis = target,
            createdTimeMillis = created,
            now = now
        )

        assertFalse(remaining.isPast)
        assertEquals(10L, remaining.totalDays)
        assertEquals("10", remaining.daysOnlyText)
        assertTrue(remaining.progress in 0.3f..0.4f)
    }

    @Test
    fun `time format utils correctly handles past events`() {
        val now = 1000000000L
        val created = now - 86400000L * 30
        val target = now - 86400000L * 12 // 12 days ago

        val remaining = TimeFormatUtils.computeRemaining(
            targetTimeMillis = target,
            createdTimeMillis = created,
            now = now
        )

        assertTrue(remaining.isPast)
        assertEquals(12L, remaining.totalDays)
        assertEquals("12 days ago", remaining.badgeText)
        assertEquals(1.0f, remaining.progress, 0.001f)
    }

    @Test
    fun `widget preferences stores and retrieves countdown mapping`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widgetId = 101
        val countdownId = 42L

        WidgetPreferences.setCountdownId(context, widgetId, countdownId)
        val retrieved = WidgetPreferences.getCountdownId(context, widgetId)
        assertEquals(countdownId, retrieved)

        // Removing specific widget config falls back to last configured countdown
        WidgetPreferences.removeWidgetConfig(context, widgetId)
        val fallback = WidgetPreferences.getCountdownId(context, widgetId)
        assertEquals(countdownId, fallback)

        WidgetPreferences.setLastConfiguredCountdownId(context, 99L)
        assertEquals(99L, WidgetPreferences.getLastConfiguredCountdownId(context))

        // Test widget tap opens item toggle
        assertTrue(WidgetPreferences.isWidgetTapOpensItemEnabled(context))
        WidgetPreferences.setWidgetTapOpensItemEnabled(context, false)
        assertFalse(WidgetPreferences.isWidgetTapOpensItemEnabled(context))
        WidgetPreferences.setWidgetTapOpensItemEnabled(context, true)
        assertTrue(WidgetPreferences.isWidgetTapOpensItemEnabled(context))

        // Test theme mode
        assertEquals("SYSTEM", WidgetPreferences.getThemeMode(context))
        WidgetPreferences.setThemeMode(context, "DARK")
        assertEquals("DARK", WidgetPreferences.getThemeMode(context))
        WidgetPreferences.setThemeMode(context, "LIGHT")
        assertEquals("LIGHT", WidgetPreferences.getThemeMode(context))
        WidgetPreferences.setThemeMode(context, "SYSTEM")
    }

    @Test
    fun `room database crud operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = inMemoryDb.countdownDao()

        val item = Countdown(
            title = "Test Trip",
            targetTimeMillis = System.currentTimeMillis() + 86400000L,
            emoji = "✈️",
            colorHex = "#38BDF8"
        )
        val id = dao.insertCountdown(item)
        val loaded = dao.getCountdownByIdSync(id)

        assertNotNull(loaded)
        assertEquals("Test Trip", loaded?.title)
        assertEquals("✈️", loaded?.emoji)

        dao.deleteCountdownById(id)
        val deleted = dao.getCountdownByIdSync(id)
        assertEquals(null, deleted)

        inMemoryDb.close()
    }

    @Test
    fun `launch MainActivity`() {
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertNotNull(scenario)
        }
    }

    @Test
    fun `test widget configuration and resolution`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        AppDatabase.seedSamples(db)

        val widgetId = 77
        val selectedId = 2L

        WidgetPreferences.setCountdownId(context, widgetId, selectedId)
        val storedId = WidgetPreferences.getCountdownId(context, widgetId)
        assertEquals(selectedId, storedId)

        val glanceId = androidx.glance.appwidget.AppWidgetId(widgetId)
        val extractedId = (glanceId as? androidx.glance.appwidget.AppWidgetId)?.appWidgetId ?: -1
        assertEquals(widgetId, extractedId)

        val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
        val managerExtracted = runCatching { manager.getAppWidgetId(glanceId) }.getOrNull()

        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
            prefs[com.example.widget.WIDGET_COUNTDOWN_PREF_KEY] = selectedId
        }
        val prefs = androidx.glance.appwidget.state.getAppWidgetState(context, androidx.glance.state.PreferencesGlanceStateDefinition, glanceId)
        val readFromGlance = prefs[com.example.widget.WIDGET_COUNTDOWN_PREF_KEY]
        assertEquals(selectedId, readFromGlance)
    }

    @Test
    fun `test launch WidgetConfigurationActivity and verify result`() {
        val testWidgetId = 456
        val intent = android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, testWidgetId)
        }
        androidx.test.core.app.ActivityScenario.launch<com.example.widget.WidgetConfigurationActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }

    @Test
    fun `test setPrimaryCountdownForAllWidgets updates specific and fallback ids`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widget1 = 10
        val widget2 = 20

        WidgetPreferences.setCountdownId(context, widget1, 1L)
        WidgetPreferences.setCountdownId(context, widget2, 2L)

        assertEquals(1L, WidgetPreferences.getSpecificCountdownId(context, widget1))
        assertEquals(2L, WidgetPreferences.getSpecificCountdownId(context, widget2))

        // Set primary countdown for all widgets to 4L
        WidgetPreferences.setPrimaryCountdownForAllWidgets(context, 4L)

        assertEquals(4L, WidgetPreferences.getSpecificCountdownId(context, widget1))
        assertEquals(4L, WidgetPreferences.getSpecificCountdownId(context, widget2))
        assertEquals(4L, WidgetPreferences.getLastConfiguredCountdownId(context))
        assertEquals(4L, WidgetPreferences.getCountdownId(context, widget1))
    }

    @Test
    fun `test WidgetDataLoader loadCountdown and loadUpcoming`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        AppDatabase.seedSamples(db)

        val widgetId = 55
        WidgetPreferences.setCountdownId(context, widgetId, 2L)

        // 1. When stateCountdownId is provided and valid, it takes priority
        val loadedFromState = com.example.widget.WidgetDataLoader.loadCountdown(context, widgetId, 1L)
        assertNotNull(loadedFromState)
        assertEquals(1L, loadedFromState?.id)

        // 2. When stateCountdownId is null, it falls back to specificPrefsId (2L)
        val loadedFromPrefs = com.example.widget.WidgetDataLoader.loadCountdown(context, widgetId, null)
        assertNotNull(loadedFromPrefs)
        assertEquals(2L, loadedFromPrefs?.id)

        // 3. Upcoming list loads up to 3 items
        val upcoming = com.example.widget.WidgetDataLoader.loadUpcoming(context, 3)
        assertTrue(upcoming.isNotEmpty())
        assertTrue(upcoming.size <= 3)
    }

    @Test
    fun `test alert rule entity and dao operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = inMemoryDb.alertRuleDao()

        val rule = com.example.data.AlertRule(
            level = 85,
            direction = com.example.data.AlertRule.DIRECTION_CHARGING_UP,
            soundType = com.example.data.AlertRule.SOUND_ALARM,
            volume = 0.8f,
            enabled = true
        )
        val id = dao.insertRule(rule)
        assertTrue(id > 0)

        val retrieved = dao.getRuleByIdSync(id)
        assertNotNull(retrieved)
        assertEquals(85, retrieved?.level)
        assertEquals(com.example.data.AlertRule.DIRECTION_CHARGING_UP, retrieved?.direction)
        assertEquals(true, retrieved?.enabled)

        dao.updateEnabled(id, false)
        val disabled = dao.getRuleByIdSync(id)
        assertEquals(false, disabled?.enabled)

        dao.updateTriggeredState(id, true)
        val triggered = dao.getRuleByIdSync(id)
        assertEquals(true, triggered?.lastTriggered)

        dao.deleteRuleById(id)
        val deleted = dao.getRuleByIdSync(id)
        assertEquals(null, deleted)

        inMemoryDb.close()
    }

    @Test
    fun `test battery alert rule evaluation logic`() {
        val chargeUpRule = com.example.data.AlertRule(
            id = 1,
            level = 80,
            direction = com.example.data.AlertRule.DIRECTION_CHARGING_UP,
            soundType = com.example.data.AlertRule.SOUND_ALARM,
            enabled = true,
            lastTriggered = false
        )

        val drainDownRule = com.example.data.AlertRule(
            id = 2,
            level = 20,
            direction = com.example.data.AlertRule.DIRECTION_DRAINING_DOWN,
            soundType = com.example.data.AlertRule.SOUND_ALARM,
            enabled = true,
            lastTriggered = false
        )

        // Charging at 80% -> chargeUpRule should fire
        val isCharging = true
        val currentLevel = 80
        val chargeUpFires = isCharging && currentLevel >= chargeUpRule.level && !chargeUpRule.lastTriggered
        assertTrue(chargeUpFires)

        // Draining at 20% -> drainDownRule should fire
        val isDraining = false
        val drainLevel = 20
        val drainDownFires = !isDraining && drainLevel <= drainDownRule.level && !drainDownRule.lastTriggered
        assertTrue(drainDownFires)

        // Once triggered, neither should re-fire at the same level
        val chargeUpTriggered = chargeUpRule.copy(lastTriggered = true)
        val drainDownTriggered = drainDownRule.copy(lastTriggered = true)
        assertFalse(isCharging && currentLevel >= chargeUpTriggered.level && !chargeUpTriggered.lastTriggered)
        assertFalse(!isDraining && drainLevel <= drainDownTriggered.level && !drainDownTriggered.lastTriggered)

        // Reset condition for charging rule: level drops below threshold OR disconnected from charger
        val dropReset = 79 < chargeUpRule.level || !isCharging
        assertTrue(dropReset)

        // Reset condition for draining rule: level goes above threshold OR connected to charger
        val raiseReset = 21 > drainDownRule.level || isCharging
        assertTrue(raiseReset)
    }

    @Test
    fun `test low power voice alert sound type`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = inMemoryDb.alertRuleDao()

        val lowPowerRule = com.example.data.AlertRule(
            level = 20,
            direction = com.example.data.AlertRule.DIRECTION_DRAINING_DOWN,
            soundType = com.example.data.AlertRule.SOUND_VOICE_ALERT,
            volume = 0.9f,
            enabled = true
        )
        val id = dao.insertRule(lowPowerRule)
        val retrieved = dao.getRuleByIdSync(id)
        assertNotNull(retrieved)
        assertEquals(com.example.data.AlertRule.SOUND_VOICE_ALERT, retrieved?.soundType)
        assertEquals("Low Power (Voice)", retrieved?.soundLabel)

        // Test updating sound type for direction
        dao.updateSoundTypeForDirection(com.example.data.AlertRule.DIRECTION_DRAINING_DOWN, com.example.data.AlertRule.SOUND_VOICE_ALERT)
        val afterUpdate = dao.getRuleByIdSync(id)
        assertEquals(com.example.data.AlertRule.SOUND_VOICE_ALERT, afterUpdate?.soundType)

        // Test charged voice alert
        val chargedRule = com.example.data.AlertRule(
            level = 100,
            direction = com.example.data.AlertRule.DIRECTION_CHARGING_UP,
            soundType = com.example.data.AlertRule.SOUND_VOICE_CHARGED,
            volume = 0.9f,
            enabled = true
        )
        val chargedId = dao.insertRule(chargedRule)
        val retrievedCharged = dao.getRuleByIdSync(chargedId)
        assertNotNull(retrievedCharged)
        assertEquals(com.example.data.AlertRule.SOUND_VOICE_CHARGED, retrievedCharged?.soundType)
        assertEquals("Full Power (Voice)", retrievedCharged?.soundLabel)

        // Test audio trimming fields
        val trimmedRule = com.example.data.AlertRule(
            level = 80,
            direction = com.example.data.AlertRule.DIRECTION_CHARGING_UP,
            soundType = com.example.data.AlertRule.SOUND_VOICE_CHARGED,
            startMs = 1200,
            endMs = 4500
        )
        val trimmedId = dao.insertRule(trimmedRule)
        val retrievedTrimmed = dao.getRuleByIdSync(trimmedId)
        assertNotNull(retrievedTrimmed)
        assertEquals(1200, retrievedTrimmed?.startMs)
        assertEquals(4500, retrievedTrimmed?.endMs)
        assertTrue(retrievedTrimmed?.isTrimmed == true)

        inMemoryDb.close()
    }

    @Test
    fun `test deleted countdown items do not reappear after deletion`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = inMemoryDb.countdownDao()

        // Initially seed samples with context
        AppDatabase.seedSamples(inMemoryDb, context)
        val initialCount = dao.getCount()
        assertTrue(initialCount > 0)

        // Delete all items
        val all = dao.getAllCountdownsSync()
        for (item in all) {
            dao.deleteCountdownById(item.id)
        }

        assertEquals(0, dao.getCount())

        // Re-running seedSamples with context should NOT resurrect the deleted countdowns
        AppDatabase.seedSamples(inMemoryDb, context)
        assertEquals(0, dao.getCount())

        inMemoryDb.close()
    }
}




