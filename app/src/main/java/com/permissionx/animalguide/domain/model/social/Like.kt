package com.permissionx.animalguide.domain.model.social

data class Like(
    val id: String = "",
    val uid: String = "",
    val targetId: String = "",
    val targetType: String = "",
    val createdAt: Long = 0L
)