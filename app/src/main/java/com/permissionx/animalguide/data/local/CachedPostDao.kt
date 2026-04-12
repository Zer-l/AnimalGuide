package com.permissionx.animalguide.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.permissionx.animalguide.data.local.entity.CachedPostEntity

@Dao
interface CachedPostDao {

    @Query("SELECT * FROM cached_posts WHERE sortType = :sortType ORDER BY position ASC")
    suspend fun getPostsBySortType(sortType: String): List<CachedPostEntity>

    @Query("SELECT * FROM cached_posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: String): CachedPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<CachedPostEntity>)

    /** 刷新缓存前先清空对应 sortType 的旧数据 */
    @Query("DELETE FROM cached_posts WHERE sortType = :sortType")
    suspend fun deletePostsBySortType(sortType: String)
}
