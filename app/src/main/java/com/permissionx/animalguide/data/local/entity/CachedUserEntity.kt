package com.permissionx.animalguide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.permissionx.animalguide.domain.model.social.User

@Entity(tableName = "cached_users")
data class CachedUserEntity(
    @PrimaryKey val uid: String,
    val nickname: String,
    val avatarUrl: String,
    val backgroundUrl: String,
    val bio: String,
    val phone: String,
    val gender: String,
    val postCount: Int,
    val followCount: Int,
    val followerCount: Int,
    val likeCount: Int,
    val cachedAt: Long
) {
    fun toUser() = User(
        id = uid,
        nickname = nickname,
        avatarUrl = avatarUrl,
        backgroundUrl = backgroundUrl,
        bio = bio,
        phone = phone,
        gender = gender,
        postCount = postCount,
        followCount = followCount,
        followerCount = followerCount,
        likeCount = likeCount
    )
}

fun User.toCacheEntity() = CachedUserEntity(
    uid = id,
    nickname = nickname,
    avatarUrl = avatarUrl,
    backgroundUrl = backgroundUrl,
    bio = bio,
    phone = phone,
    gender = gender,
    postCount = postCount,
    followCount = followCount,
    followerCount = followerCount,
    likeCount = likeCount,
    cachedAt = System.currentTimeMillis()
)
