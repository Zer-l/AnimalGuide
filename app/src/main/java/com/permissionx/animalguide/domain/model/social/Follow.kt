package com.permissionx.animalguide.domain.model.social

data class Follow(
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val createdAt: Long = 0L
)