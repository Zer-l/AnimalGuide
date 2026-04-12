package com.permissionx.animalguide.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.permissionx.animalguide.data.local.entity.CachedUserEntity

@Dao
interface CachedUserDao {

    @Query("SELECT * FROM cached_users WHERE uid = :uid LIMIT 1")
    suspend fun getUserByUid(uid: String): CachedUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: CachedUserEntity)
}
