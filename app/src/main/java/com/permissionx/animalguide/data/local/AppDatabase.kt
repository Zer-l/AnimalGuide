package com.permissionx.animalguide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.data.local.entity.RecognizeHistory
import com.permissionx.animalguide.data.local.entity.AnimalPhoto

@Database(
    entities = [AnimalEntry::class, RecognizeHistory::class, AnimalPhoto::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun historyDao(): HistoryDao
    abstract fun animalPhotoDao(): AnimalPhotoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pokedex ADD COLUMN lastSeenAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE history ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE history ADD COLUMN longitude REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS animal_photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        animalName TEXT NOT NULL,
                        imageUri TEXT NOT NULL,
                        takenAt INTEGER NOT NULL
                    )
                """.trimIndent()
                )
            }
        }
    }
}