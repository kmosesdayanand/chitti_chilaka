package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate4To5() {
        var db = helper.createDatabase(TEST_DB, 4).apply {
            execSQL("INSERT INTO countdowns (id, title, targetTimeMillis, createdTimeMillis, emoji, colorHex, notes, category, cardStyle, displayUnit) VALUES (1, 'Launch Event', 2000000000000, 1000000000000, '🚀', '#06B6D4', '', 'General', 'classic', 'auto')")
            execSQL("INSERT INTO alert_rules (id, level, direction, soundType, volume, enabled, lastTriggered, startMs, endMs) VALUES (1, 80, 'CHARGING_UP', 'SYSTEM_DEFAULT', 1.0, 1, 0, 0, 0)")
            close()
        }

        // Re-open database with version 5 and apply MIGRATION_4_5
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5)

        // Verify pre-existing data was preserved
        val countdownCursor = db.query("SELECT title FROM countdowns WHERE id = 1")
        assertTrue(countdownCursor.moveToFirst())
        assertEquals("Launch Event", countdownCursor.getString(0))
        countdownCursor.close()

        val alertCursor = db.query("SELECT level FROM alert_rules WHERE id = 1")
        assertTrue(alertCursor.moveToFirst())
        assertEquals(80, alertCursor.getInt(0))
        alertCursor.close()

        // Verify vip_contacts table was created by inserting and querying a row
        db.execSQL("INSERT INTO vip_contacts (id, contactName, phoneNumber, normalizedNumber, enabled, createdAt, photoUri) VALUES (1, 'Emergency Contact', '+1234567890', '1234567890', 1, 1000000000, null)")
        val vipCursor = db.query("SELECT contactName, phoneNumber FROM vip_contacts WHERE id = 1")
        assertTrue(vipCursor.moveToFirst())
        assertEquals("Emergency Contact", vipCursor.getString(0))
        assertEquals("+1234567890", vipCursor.getString(1))
        vipCursor.close()
    }
}
