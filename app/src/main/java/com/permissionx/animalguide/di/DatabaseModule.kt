package com.permissionx.animalguide.di

import android.content.Context
import androidx.room.Room
import com.permissionx.animalguide.data.local.AnimalDao
import com.permissionx.animalguide.data.local.AnimalPhotoDao
import com.permissionx.animalguide.data.local.AppDatabase
import com.permissionx.animalguide.data.local.CachedPostDao
import com.permissionx.animalguide.data.local.CachedUserDao
import com.permissionx.animalguide.data.local.ChatConversationDao
import com.permissionx.animalguide.data.local.ChatMessageDao
import com.permissionx.animalguide.data.local.HistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "animal_guide.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
            .build()
    }

    @Provides
    @Singleton
    fun provideAnimalDao(database: AppDatabase): AnimalDao {
        return database.animalDao()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Suppress("unused")
    @Provides
    @Singleton
    fun provideAnimalPhotoDao(database: AppDatabase): AnimalPhotoDao {
        return database.animalPhotoDao()
    }

    @Provides
    @Singleton
    fun provideCachedPostDao(database: AppDatabase): CachedPostDao {
        return database.cachedPostDao()
    }

    @Provides
    @Singleton
    fun provideCachedUserDao(database: AppDatabase): CachedUserDao {
        return database.cachedUserDao()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    @Singleton
    fun provideChatConversationDao(database: AppDatabase): ChatConversationDao {
        return database.chatConversationDao()
    }
}