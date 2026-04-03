package com.permissionx.animalguide.ui.me

import com.permissionx.animalguide.domain.model.social.Post
import com.permissionx.animalguide.domain.model.social.User

sealed class MeUiState {
    object Guest : MeUiState()
    object Loading : MeUiState()
    data class Success(
        val user: User,
        val posts: List<Post> = emptyList(),
        val hasMorePosts: Boolean = false,
        val isLoadingMorePosts: Boolean = false,
        val selectedTab: Int = 0
    ) : MeUiState()

    data class Error(val message: String) : MeUiState()
}