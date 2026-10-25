package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(entities = [Countdown::class, AlertRule::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun countdownDao(): CountdownDao
    abstract fun alertRuleDao(): AlertRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phone_manager_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedSamples(getInstance(context.applicationContext))
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedSamples(getInstance(context.applicationContext))
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedSamples(database: AppDatabase) {
            val countdownDao = database.countdownDao()
            if (countdownDao.getCount() == 0) {
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

            val alertDao = database.alertRuleDao()
            if (alertDao.getCount() == 0) {
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
    }
}
