package com.permissionx.animalguide.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.permissionx.animalguide.data.local.entity.CachedCommentEntity

@Dao
interface CachedCommentDao {

    @Query("SELECT * FROM cached_comments WHERE postId = :postId ORDER BY position ASC")
    suspend fun getCommentsByPostId(postId: String): List<CachedCommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CachedCommentEntity>)

    @Query("DELETE FROM cached_comments WHERE postId = :postId")
    suspend fun deleteByPostId(postId: String)
}
