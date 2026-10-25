package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(entities = [Countdown::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun countdownDao(): CountdownDao

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
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedSamples(database: AppDatabase) {
            val dao = database.countdownDao()
            if (dao.getCount() > 0) return

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

            samples.forEach { dao.insertCountdown(it) }
        }
    }
}
