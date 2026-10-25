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
        assertEquals("Phone Manager", appName)
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
}
