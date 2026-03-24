package com.permissionx.animalguide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.data.local.entity.RecognizeHistory

@Database(
    entities = [AnimalEntry::class, RecognizeHistory::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun historyDao(): HistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // pokedex 表新增字段
                db.execSQL("ALTER TABLE pokedex ADD COLUMN lastSeenAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN note TEXT NOT NULL DEFAULT ''")

                // history 表新增字段
                db.execSQL("ALTER TABLE history ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE history ADD COLUMN longitude REAL")
            }
        }
    }
}