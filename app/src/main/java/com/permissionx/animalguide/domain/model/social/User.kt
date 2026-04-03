package com.permissionx.animalguide.domain.model.social

data class User(
    val id: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val phone: String = "",
    val gender: String = "SECRET",  // 新增
    val postCount: Int = 0,
    val followCount: Int = 0,
    val followerCount: Int = 0,
    val likeCount: Int = 0
)

enum class Gender(val value: String, val label: String) {
    MALE("MALE", "男"),
    FEMALE("FEMALE", "女"),
    SECRET("SECRET", "保密")
}