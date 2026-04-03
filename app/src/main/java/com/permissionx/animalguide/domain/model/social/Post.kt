package com.permissionx.animalguide.domain.model.social

data class Post(
    val id: String = "",
    val uid: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val type: PostType = PostType.ORIGINAL,
    val animalName: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val collectCount: Int = 0,
    val coverUrl: String = "",
    val status: PostStatus = PostStatus.NORMAL,
    val createdAt: Long = 0L,
    val isLiked: Boolean = false,
    val isCollected: Boolean = false
)

enum class PostType { ANIMAL_SHARE, ORIGINAL }
enum class PostStatus { NORMAL, DELETED, BLOCKED }