package com.permissionx.animalguide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.data.local.entity.RecognizeHistory
import com.permissionx.animalguide.data.local.entity.AnimalPhoto
import com.permissionx.animalguide.data.local.entity.CachedCommentEntity
import com.permissionx.animalguide.data.local.entity.CachedPostEntity
import com.permissionx.animalguide.data.local.entity.CachedUserEntity
import com.permissionx.animalguide.data.local.entity.ChatConversationEntity
import com.permissionx.animalguide.data.local.entity.ChatMessageEntity

@Database(
    entities = [AnimalEntry::class, RecognizeHistory::class, AnimalPhoto::class,
        CachedPostEntity::class, CachedUserEntity::class,
        ChatMessageEntity::class, ChatConversationEntity::class,
        CachedCommentEntity::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun historyDao(): HistoryDao
    abstract fun animalPhotoDao(): AnimalPhotoDao
    abstract fun cachedPostDao(): CachedPostDao
    abstract fun cachedUserDao(): CachedUserDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatConversationDao(): ChatConversationDao
    abstract fun cachedCommentDao(): CachedCommentDao

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

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pokedex ADD COLUMN taxonomy TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN distribution TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN morphology TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN activityPattern TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN socialBehavior TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN ecologicalRole TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pokedex ADD COLUMN funFacts TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // cached_posts 新增 isLiked / isCollected 字段
                db.execSQL("ALTER TABLE cached_posts ADD COLUMN isLiked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cached_posts ADD COLUMN isCollected INTEGER NOT NULL DEFAULT 0")
                // 新增评论缓存表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_comments (
                        id TEXT PRIMARY KEY NOT NULL,
                        postId TEXT NOT NULL,
                        uid TEXT NOT NULL,
                        nickname TEXT NOT NULL,
                        avatarUrl TEXT NOT NULL,
                        content TEXT NOT NULL,
                        parentId TEXT NOT NULL,
                        replyToUid TEXT NOT NULL,
                        replyToNickname TEXT NOT NULL,
                        likeCount INTEGER NOT NULL,
                        replyCount INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        position INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 重建 chat_messages 表，将 animalName 改为 conversationId
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_messages_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        conversationId TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO chat_messages_new SELECT id, animalName, role, content, timestamp FROM chat_messages"
                )
                db.execSQL("DROP TABLE chat_messages")
                db.execSQL("ALTER TABLE chat_messages_new RENAME TO chat_messages")

                // 新建对话会话表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_conversations (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastMessageAt INTEGER NOT NULL,
                        previewText TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        animalName TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_posts (
                        id TEXT PRIMARY KEY NOT NULL,
                        uid TEXT NOT NULL,
                        nickname TEXT NOT NULL,
                        avatarUrl TEXT NOT NULL,
                        type TEXT NOT NULL,
                        animalName TEXT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        imageUrls TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        location TEXT NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        likeCount INTEGER NOT NULL,
                        commentCount INTEGER NOT NULL,
                        collectCount INTEGER NOT NULL,
                        coverUrl TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        sortType TEXT NOT NULL,
                        position INTEGER NOT NULL
                    )
                """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_users (
                        uid TEXT PRIMARY KEY NOT NULL,
                        nickname TEXT NOT NULL,
                        avatarUrl TEXT NOT NULL,
                        backgroundUrl TEXT NOT NULL,
                        bio TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        gender TEXT NOT NULL,
                        postCount INTEGER NOT NULL,
                        followCount INTEGER NOT NULL,
                        followerCount INTEGER NOT NULL,
                        likeCount INTEGER NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                """.trimIndent()
                )
            }
        }
    }
}