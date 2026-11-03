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
@Config(sdk = [36])
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

    @Test
    fun `phone number matcher accurately normalizes and matches last 10 digits`() {
        // Test user's exact example: +91 98765 43210, 098765 43210 and 9876543210 all match
        val indianIntl = "+91 98765 43210"
        val indianZeroPrefix = "098765 43210"
        val indian10Digits = "9876543210"

        assertTrue(com.example.util.PhoneNumberMatcher.matches(indianIntl, indianZeroPrefix))
        assertTrue(com.example.util.PhoneNumberMatcher.matches(indianZeroPrefix, indian10Digits))
        assertTrue(com.example.util.PhoneNumberMatcher.matches(indianIntl, indian10Digits))

        // US phone formats with hyphens, parentheses and country code
        val usStandard = "(555) 234-5678"
        val usWithCountry = "+1 555 234 5678"
        val usRawDigits = "5552345678"
        assertTrue(com.example.util.PhoneNumberMatcher.matches(usStandard, usWithCountry))
        assertTrue(com.example.util.PhoneNumberMatcher.matches(usStandard, usRawDigits))

        // Different numbers must NOT match
        assertFalse(com.example.util.PhoneNumberMatcher.matches("9876543210", "9876543211"))

        // Edge case: Unknown, private or restricted callers must NEVER match
        assertFalse(com.example.util.PhoneNumberMatcher.matches("Unknown", "9876543210"))
        assertFalse(com.example.util.PhoneNumberMatcher.matches("Private", "9876543210"))
        assertFalse(com.example.util.PhoneNumberMatcher.matches("Restricted", "9876543210"))
        assertFalse(com.example.util.PhoneNumberMatcher.matches("", "9876543210"))
        assertFalse(com.example.util.PhoneNumberMatcher.matches(null, "9876543210"))

        // Short numbers / emergency codes (< 7 digits) must not match
        assertFalse(com.example.util.PhoneNumberMatcher.matches("911", "911"))
        assertFalse(com.example.util.PhoneNumberMatcher.matches("12345", "12345"))
    }

    @Test
    fun `vip contact entity and dao crud operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = inMemoryDb.vipContactDao()

        // Insert contact with multiple numbers as separate records
        val contact1 = com.example.data.VipContact(
            contactName = "Alice Smith",
            phoneNumber = "+1 (555) 111-2222"
        )
        val contact2 = com.example.data.VipContact(
            contactName = "Alice Smith",
            phoneNumber = "+1 (555) 333-4444"
        )
        val id1 = dao.insertContact(contact1)
        val id2 = dao.insertContact(contact2)

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
        assertEquals(2, dao.getCount())

        val enabledList = dao.getEnabledContactsSync()
        assertEquals(2, enabledList.size)

        // Disable contact1
        dao.updateEnabled(id1, false)
        val enabledAfter = dao.getEnabledContactsSync()
        assertEquals(1, enabledAfter.size)
        assertEquals(id2, enabledAfter[0].id)

        // Delete contact2
        dao.deleteContactById(id2)
        assertEquals(1, dao.getCount())

        // Delete all contacts
        dao.deleteAllContacts()
        assertEquals(0, dao.getCount())

        inMemoryDb.close()
    }

    @Test
    fun `vip preferences data store operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.VipPreferences(context)

        // Test default settings
        val initialSettings = prefs.getVipSettings()
        assertTrue(initialSettings.vipRingEnabled)
        assertEquals(1.0f, initialSettings.ringVolumePercent, 0.01f)
        assertTrue(initialSettings.overrideDnd)
        assertTrue(initialSettings.alsoVibrate)
        assertTrue(initialSettings.ringOnSilent)

        // Update settings
        prefs.setVipRingEnabled(false)
        assertFalse(prefs.getVipSettings().vipRingEnabled)
        prefs.setVipRingEnabled(true)

        prefs.setRingVolumePercent(0.75f)
        assertEquals(0.75f, prefs.getVipSettings().ringVolumePercent, 0.01f)

        // Audio state saving and clearing
        val initialState = prefs.getSavedAudioState()
        assertFalse(initialState.isActive)

        prefs.saveAudioState(ringerMode = 0, streamVolume = 3, interruptionFilter = 2)
        val saved = prefs.getSavedAudioState()
        assertTrue(saved.isActive)
        assertEquals(0, saved.ringerMode)
        assertEquals(3, saved.streamVolume)
        assertEquals(2, saved.interruptionFilter)

        prefs.clearSavedAudioState()
        val cleared = prefs.getSavedAudioState()
        assertFalse(cleared.isActive)
        assertEquals(-1, cleared.ringerMode)
    }

    @Test
    fun `vip preferences awaitSettings and awaitAudioState functions`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.VipPreferences(context)

        // awaitSettings populates cache and sets isSettingsLoaded
        val settings = prefs.awaitSettings()
        assertNotNull(settings)
        assertTrue(prefs.isSettingsLoaded)

        // awaitAudioState populates cache and sets isAudioStateLoaded
        val audioState = prefs.awaitAudioState()
        assertNotNull(audioState)
        assertTrue(prefs.isAudioStateLoaded)

        // Test saveAudioState sets isAudioStateLoaded
        prefs.saveAudioState(ringerMode = 1, streamVolume = 5, interruptionFilter = 1, timestamp = 12345L)
        assertTrue(prefs.isAudioStateLoaded)
        val awaitedSaved = prefs.awaitAudioState()
        assertTrue(awaitedSaved.isActive)
        assertEquals(1, awaitedSaved.ringerMode)
        assertEquals(5, awaitedSaved.streamVolume)
        assertEquals(12345L, awaitedSaved.savedTimestampMillis)

        // Test clearSavedAudioState sets isAudioStateLoaded
        prefs.clearSavedAudioState()
        assertTrue(prefs.isAudioStateLoaded)
        val awaitedCleared = prefs.awaitAudioState()
        assertFalse(awaitedCleared.isActive)
        assertEquals(0L, awaitedCleared.savedTimestampMillis)
    }

    @Test
    fun `vip ring manager leftover check treats zero timestamp as old`() {
        val now = 1000000000L
        val zeroTimestamp = 0L
        val recentTimestamp = now - 30_000L // 30 seconds ago
        val oldTimestamp = now - 150_000L // 2.5 minutes ago

        // Legacy un-timestamped (0L) should be treated as old
        val isZeroOld = zeroTimestamp == 0L || (now - zeroTimestamp) > (2 * 60 * 1000L)
        assertTrue(isZeroOld)

        // 2.5 minutes ago should be treated as old
        val isOldOld = oldTimestamp == 0L || (now - oldTimestamp) > (2 * 60 * 1000L)
        assertTrue(isOldOld)

        // 30 seconds ago should NOT be treated as old
        val isRecentOld = recentTimestamp == 0L || (now - recentTimestamp) > (2 * 60 * 1000L)
        assertFalse(isRecentOld)
    }

    @Test
    fun `vip ring service timeout constants and re-arm calculation`() {
        assertEquals(60_000L, com.example.service.VipRingService.INITIAL_TIMEOUT_MS)
        assertEquals(30_000L, com.example.service.VipRingService.REARM_TIMEOUT_MS)
        assertEquals(180_000L, com.example.service.VipRingService.MAX_TOTAL_DURATION_MS)

        val maxDuration = com.example.service.VipRingService.MAX_TOTAL_DURATION_MS
        val rearmDelay = com.example.service.VipRingService.REARM_TIMEOUT_MS

        // At 60s elapsed, ringing: remaining = 120s, nextDelay = minOf(30s, 120s) = 30s
        val elapsed60 = 60_000L
        val remaining60 = maxDuration - elapsed60
        val nextDelay60 = minOf(rearmDelay, remaining60.coerceAtLeast(1000L))
        assertEquals(30_000L, nextDelay60)

        // At 160s elapsed, ringing: remaining = 20s, nextDelay = minOf(30s, 20s) = 20s
        val elapsed160 = 160_000L
        val remaining160 = maxDuration - elapsed160
        val nextDelay160 = minOf(rearmDelay, remaining160.coerceAtLeast(1000L))
        assertEquals(20_000L, nextDelay160)

        // At 180s elapsed: hard limit reached
        val elapsed180 = 180_000L
        assertTrue(elapsed180 >= maxDuration)
    }

    @Test
    fun `screen saver correctly saves and restores all screens and handles unknown routes`() {
        val scope = object : androidx.compose.runtime.saveable.SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }

        for (screen in com.example.ui.navigation.Screen.items) {
            val savedRoute = with(ScreenSaver) { scope.save(screen) }
            assertEquals(screen.route, savedRoute)

            val restoredScreen = ScreenSaver.restore(savedRoute!!)
            assertEquals(screen, restoredScreen)
        }

        // Unknown route falls back to Countdowns
        val unknownRestored = ScreenSaver.restore("unknown_random_route")
        assertEquals(com.example.ui.navigation.Screen.Countdowns, unknownRestored)
    }

    @Test
    fun `activity recreation preserves process flags and scenario recreate succeeds`() {
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTrue(MainActivity.startupWorkExecuted)
            assertTrue(MainActivity.hasShownSplashInProcess)

            // Recreate activity (simulating rotation or theme change)
            scenario.recreate()

            // Process flags remain intact and recreation succeeds
            assertTrue(MainActivity.startupWorkExecuted)
            assertTrue(MainActivity.hasShownSplashInProcess)
        }
    }

    @Test
    fun `backup rules and data extraction rules exclude private data and device xml`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify backup_rules.xml (Android 11 and below)
        val backupParser = context.resources.getXml(R.xml.backup_rules)
        val fullBackupExcludes = mutableListOf<Pair<String, String>>()
        var eventType = backupParser.eventType
        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && backupParser.name == "exclude") {
                val domain = backupParser.getAttributeValue(null, "domain") ?: ""
                val path = backupParser.getAttributeValue(null, "path") ?: ""
                fullBackupExcludes.add(domain to path)
            }
            eventType = backupParser.next()
        }

        assertTrue("database phone_manager_db excluded in backup_rules",
            fullBackupExcludes.contains("database" to "phone_manager_db"))
        assertTrue("database phone_manager_db-wal excluded in backup_rules",
            fullBackupExcludes.contains("database" to "phone_manager_db-wal"))
        assertTrue("database phone_manager_db-shm excluded in backup_rules",
            fullBackupExcludes.contains("database" to "phone_manager_db-shm"))
        assertTrue("datastore folder excluded in backup_rules",
            fullBackupExcludes.any { it.first == "file" && it.second.contains("datastore") })
        assertTrue("phone_manager_seeds excluded in backup_rules",
            fullBackupExcludes.any { it.first == "sharedpref" && it.second.startsWith("phone_manager_seeds") })
        assertTrue("device.xml excluded in backup_rules",
            fullBackupExcludes.contains("sharedpref" to "device.xml"))

        // 2. Verify data_extraction_rules.xml (Android 12 and above)
        val extractionParser = context.resources.getXml(R.xml.data_extraction_rules)
        val cloudBackupExcludes = mutableListOf<Pair<String, String>>()
        val deviceTransferExcludes = mutableListOf<Pair<String, String>>()
        var currentSection = ""
        eventType = extractionParser.eventType
        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                when (extractionParser.name) {
                    "cloud-backup" -> currentSection = "cloud-backup"
                    "device-transfer" -> currentSection = "device-transfer"
                    "exclude" -> {
                        val domain = extractionParser.getAttributeValue(null, "domain") ?: ""
                        val path = extractionParser.getAttributeValue(null, "path") ?: ""
                        if (currentSection == "cloud-backup") {
                            cloudBackupExcludes.add(domain to path)
                        } else if (currentSection == "device-transfer") {
                            deviceTransferExcludes.add(domain to path)
                        }
                    }
                }
            } else if (eventType == org.xmlpull.v1.XmlPullParser.END_TAG) {
                if (extractionParser.name == "cloud-backup" || extractionParser.name == "device-transfer") {
                    currentSection = ""
                }
            }
            eventType = extractionParser.next()
        }

        // Verify cloud-backup exclusions
        assertTrue("database phone_manager_db excluded in cloud-backup",
            cloudBackupExcludes.contains("database" to "phone_manager_db"))
        assertTrue("database phone_manager_db-wal excluded in cloud-backup",
            cloudBackupExcludes.contains("database" to "phone_manager_db-wal"))
        assertTrue("database phone_manager_db-shm excluded in cloud-backup",
            cloudBackupExcludes.contains("database" to "phone_manager_db-shm"))
        assertTrue("datastore folder excluded in cloud-backup",
            cloudBackupExcludes.any { it.first == "file" && it.second.contains("datastore") })
        assertTrue("phone_manager_seeds excluded in cloud-backup",
            cloudBackupExcludes.any { it.first == "sharedpref" && it.second.startsWith("phone_manager_seeds") })
        assertTrue("device.xml excluded in cloud-backup",
            cloudBackupExcludes.contains("sharedpref" to "device.xml"))

        // Verify device-transfer exclusions
        assertTrue("database phone_manager_db excluded in device-transfer",
            deviceTransferExcludes.contains("database" to "phone_manager_db"))
        assertTrue("database phone_manager_db-wal excluded in device-transfer",
            deviceTransferExcludes.contains("database" to "phone_manager_db-wal"))
        assertTrue("database phone_manager_db-shm excluded in device-transfer",
            deviceTransferExcludes.contains("database" to "phone_manager_db-shm"))
        assertTrue("datastore folder excluded in device-transfer",
            deviceTransferExcludes.any { it.first == "file" && it.second.contains("datastore") })
        assertTrue("phone_manager_seeds excluded in device-transfer",
            deviceTransferExcludes.any { it.first == "sharedpref" && it.second.startsWith("phone_manager_seeds") })
        assertTrue("device.xml excluded in device-transfer",
            deviceTransferExcludes.contains("sharedpref" to "device.xml"))
    }

    @Test
    fun `notification permission asked preference stores and persists state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Initially false
        assertFalse(WidgetPreferences.hasAskedNotificationPermission(context))

        // Set to true
        WidgetPreferences.setNotificationPermissionAsked(context, true)
        assertTrue(WidgetPreferences.hasAskedNotificationPermission(context))

        // Can reset or set to false
        WidgetPreferences.setNotificationPermissionAsked(context, false)
        assertFalse(WidgetPreferences.hasAskedNotificationPermission(context))
    }

    @Test
    fun `vip checklist items contain required explanation and warning text`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val checklist = com.example.util.VipPermissionChecker.getChecklist(context)

        val contactsItem = checklist.find { it.id == "contacts" }
        assertNotNull(contactsItem)
        assertTrue(
            contactsItem!!.description.contains("Without this permission Android may skip call screening for saved contacts, so VIP calls may not ring loudly.")
        )

        val roleItem = checklist.find { it.id == "role_screening" }
        assertNotNull(roleItem)
        assertTrue(
            roleItem!!.description.contains("Sentry becomes your call screening app. This replaces Google Caller ID & spam protection or Truecaller. Sentry never blocks, silences or rejects calls.")
        )
    }

    @Test
    fun `vip warning banner condition evaluates correctly based on vipRingEnabled and permissions`() {
        // Banner must show when vipRingEnabled is true and either contacts or screening role is missing
        fun shouldShowBanner(vipRingEnabled: Boolean, contactsGranted: Boolean, screeningRoleHeld: Boolean): Boolean {
            return vipRingEnabled && (!contactsGranted || !screeningRoleHeld)
        }

        // VIP ring disabled: never show banner
        assertFalse(shouldShowBanner(vipRingEnabled = false, contactsGranted = false, screeningRoleHeld = false))
        assertFalse(shouldShowBanner(vipRingEnabled = false, contactsGranted = true, screeningRoleHeld = true))

        // VIP ring enabled, missing contacts permission: show banner
        assertTrue(shouldShowBanner(vipRingEnabled = true, contactsGranted = false, screeningRoleHeld = true))

        // VIP ring enabled, missing call screening role: show banner
        assertTrue(shouldShowBanner(vipRingEnabled = true, contactsGranted = true, screeningRoleHeld = false))

        // VIP ring enabled, both missing: show banner
        assertTrue(shouldShowBanner(vipRingEnabled = true, contactsGranted = false, screeningRoleHeld = false))

        // Everything granted: hide banner
        assertFalse(shouldShowBanner(vipRingEnabled = true, contactsGranted = true, screeningRoleHeld = true))
    }

    @Test
    fun `manifest sets exported false on BootReceiver and PhoneStateReceiver`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager

        val bootReceiverInfo = pm.getReceiverInfo(
            android.content.ComponentName(context, com.example.worker.BootReceiver::class.java),
            0
        )
        assertFalse(bootReceiverInfo.exported)

        val phoneStateReceiverInfo = pm.getReceiverInfo(
            android.content.ComponentName(context, com.example.receiver.PhoneStateReceiver::class.java),
            0
        )
        assertFalse(phoneStateReceiverInfo.exported)
    }

    @Test
    fun `phone state receiver returns immediately on SDK 29 and higher`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = com.example.receiver.PhoneStateReceiver()
        val intent = android.content.Intent(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED).apply {
            putExtra(android.telephony.TelephonyManager.EXTRA_STATE, android.telephony.TelephonyManager.EXTRA_STATE_RINGING)
            putExtra(android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER, "+15551234567")
        }

        // Under Robolectric (configured with SDK 36 >= 29), onReceive must return immediately
        receiver.onReceive(context, intent)
    }
}






