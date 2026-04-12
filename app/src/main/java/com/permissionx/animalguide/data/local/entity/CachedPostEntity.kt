package com.permissionx.animalguide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.permissionx.animalguide.domain.model.social.Post
import com.permissionx.animalguide.domain.model.social.PostStatus
import com.permissionx.animalguide.domain.model.social.PostType

@Entity(tableName = "cached_posts")
data class CachedPostEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val nickname: String,
    val avatarUrl: String,
    val type: String,
    val animalName: String,
    val title: String,
    val content: String,
    /** List<String> 序列化为 "|||" 分隔的字符串 */
    val imageUrls: String,
    val tags: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val likeCount: Int,
    val commentCount: Int,
    val collectCount: Int,
    val coverUrl: String,
    val status: String,
    val createdAt: Long,
    /** "hot" 或 "latest"，区分推荐/最新两个 feed 的缓存 */
    val sortType: String,
    /** 在列表中的排序位置，保证离线时顺序正确 */
    val position: Int
) {
    fun toPost() = Post(
        id = id,
        uid = uid,
        nickname = nickname,
        avatarUrl = avatarUrl,
        type = if (type == "ANIMAL_SHARE") PostType.ANIMAL_SHARE else PostType.ORIGINAL,
        animalName = animalName,
        title = title,
        content = content,
        imageUrls = if (imageUrls.isEmpty()) emptyList() else imageUrls.split("|||"),
        tags = if (tags.isEmpty()) emptyList() else tags.split("|||"),
        location = location,
        latitude = latitude,
        longitude = longitude,
        likeCount = likeCount,
        commentCount = commentCount,
        collectCount = collectCount,
        coverUrl = coverUrl,
        status = when (status) {
            "DELETED" -> PostStatus.DELETED
            "BLOCKED" -> PostStatus.BLOCKED
            else -> PostStatus.NORMAL
        },
        createdAt = createdAt
    )
}

fun Post.toCacheEntity(sortType: String, position: Int) = CachedPostEntity(
    id = id,
    uid = uid,
    nickname = nickname,
    avatarUrl = avatarUrl,
    type = type.name,
    animalName = animalName,
    title = title,
    content = content,
    imageUrls = imageUrls.joinToString("|||"),
    tags = tags.joinToString("|||"),
    location = location,
    latitude = latitude,
    longitude = longitude,
    likeCount = likeCount,
    commentCount = commentCount,
    collectCount = collectCount,
    coverUrl = coverUrl,
    status = status.name,
    createdAt = createdAt,
    sortType = sortType,
    position = position
)
