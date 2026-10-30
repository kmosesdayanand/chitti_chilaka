package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(entities = [Countdown::class, AlertRule::class, VipContact::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun countdownDao(): CountdownDao
    abstract fun alertRuleDao(): AlertRuleDao
    abstract fun vipContactDao(): VipContactDao

    companion object {
        const val PREFS_SEEDS = "phone_manager_seeds"
        const val KEY_COUNTDOWNS_SEEDED = "countdowns_initial_seeded"
        const val KEY_ALERTS_SEEDED = "alerts_initial_seeded"

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vip_contacts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `contactName` TEXT NOT NULL,
                        `phoneNumber` TEXT NOT NULL,
                        `normalizedNumber` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL,
                        `photoUri` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "phone_manager_db"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedSamples(getInstance(appContext), appContext)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedSamples(
            database: AppDatabase,
            context: Context? = null,
            force: Boolean = false
        ) {
            val prefs = context?.getSharedPreferences(PREFS_SEEDS, Context.MODE_PRIVATE)
            val hasSeededCountdowns = prefs?.getBoolean(KEY_COUNTDOWNS_SEEDED, false) ?: false
            val countdownDao = database.countdownDao()
            val alertDao = database.alertRuleDao()

            // Countdowns: seed ONLY on fresh creation or in test environments where force/null context is used
            if (force || (context == null && countdownDao.getCount() == 0)) {
                if (countdownDao.getCount() == 0) {
                    insertDefaultCountdowns(countdownDao)
                }
            } else if (prefs != null && !hasSeededCountdowns) {
                if (countdownDao.getCount() == 0) {
                    insertDefaultCountdowns(countdownDao)
                }
                prefs.edit().putBoolean(KEY_COUNTDOWNS_SEEDED, true).apply()
            }

            // Battery Alert Rules: seed ONLY on fresh creation
            val hasSeededAlerts = prefs?.getBoolean(KEY_ALERTS_SEEDED, false) ?: false
            if (force || (context == null && alertDao.getCount() == 0)) {
                if (alertDao.getCount() == 0) {
                    insertDefaultAlertRules(alertDao)
                }
            } else if (prefs != null && !hasSeededAlerts) {
                if (alertDao.getCount() == 0) {
                    insertDefaultAlertRules(alertDao)
                }
                prefs.edit().putBoolean(KEY_ALERTS_SEEDED, true).apply()
            } else {
                alertDao.updateSoundTypeForDirection(
                    direction = AlertRule.DIRECTION_DRAINING_DOWN,
                    newSoundType = AlertRule.SOUND_VOICE_ALERT
                )
                alertDao.updateSoundTypeForDirection(
                    direction = AlertRule.DIRECTION_CHARGING_UP,
                    newSoundType = AlertRule.SOUND_VOICE_CHARGED
                )
            }
        }

        private suspend fun insertDefaultCountdowns(countdownDao: CountdownDao) {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            val nextNewYear = Calendar.getInstance().apply {
                set(Calendar.YEAR, get(Calendar.YEAR) + 1)
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val samples = listOf(
                Countdown(
                    title = "New Year's Eve Celebration",
                    targetTimeMillis = nextNewYear,
                    createdTimeMillis = now - dayMs * 20,
                    emoji = "🎆",
                    colorHex = "#A855F7",
                    notes = "Welcome the new year with friends and family!",
                    category = "Holiday",
                    cardStyle = "modern",
                    displayUnit = "all"
                ),
                Countdown(
                    title = "Summer Road Trip & Camping",
                    targetTimeMillis = now + dayMs * 14 + 18000000L,
                    createdTimeMillis = now - dayMs * 5,
                    emoji = "🏕️",
                    colorHex = "#38BDF8",
                    notes = "Tent, camping gear, camera, and hiking boots.",
                    category = "Trip",
                    cardStyle = "tinted",
                    displayUnit = "all"
                ),
                Countdown(
                    title = "Product Launch & Keynote",
                    targetTimeMillis = now + dayMs * 4 + 3600000L * 7,
                    createdTimeMillis = now - dayMs * 10,
                    emoji = "🚀",
                    colorHex = "#10B981",
                    notes = "Team demo and release announcement livestream.",
                    category = "Work",
                    cardStyle = "modern",
                    displayUnit = "hours"
                ),
                Countdown(
                    title = "Sarah's Surprise Birthday",
                    targetTimeMillis = now + dayMs * 28 + 3600000L * 14,
                    createdTimeMillis = now - dayMs * 3,
                    emoji = "🎂",
                    colorHex = "#FB7185",
                    notes = "Order customized cake and balloons.",
                    category = "Birthday",
                    cardStyle = "tinted",
                    displayUnit = "days"
                ),
                Countdown(
                    title = "College Graduation Day",
                    targetTimeMillis = now - dayMs * 35,
                    createdTimeMillis = now - dayMs * 120,
                    emoji = "🎓",
                    colorHex = "#94A3B8",
                    notes = "A memorable milestone accomplished.",
                    category = "Milestone",
                    cardStyle = "minimal",
                    displayUnit = "all"
                )
            )

            samples.forEach { countdownDao.insertCountdown(it) }
        }

        private suspend fun insertDefaultAlertRules(alertDao: AlertRuleDao) {
            alertDao.insertRule(
                AlertRule(
                    level = 80,
                    direction = AlertRule.DIRECTION_CHARGING_UP,
                    soundType = AlertRule.SOUND_VOICE_CHARGED,
                    volume = 0.8f,
                    enabled = true
                )
            )
            alertDao.insertRule(
                AlertRule(
                    level = 20,
                    direction = AlertRule.DIRECTION_DRAINING_DOWN,
                    soundType = AlertRule.SOUND_VOICE_ALERT,
                    volume = 0.8f,
                    enabled = true
                )
            )
        }
    }
}
