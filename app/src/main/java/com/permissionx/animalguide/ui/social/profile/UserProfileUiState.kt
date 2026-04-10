package com.permissionx.animalguide.ui.social.profile

import com.permissionx.animalguide.domain.model.social.Post
import com.permissionx.animalguide.domain.model.social.User

sealed class UserProfileUiState {
    object Loading : UserProfileUiState()
    data class Error(val message: String) : UserProfileUiState()
    data class Success(
        val user: User,
        val isFollowing: Boolean = false,
        val isFollowLoading: Boolean = false,
        val posts: List<Post> = emptyList(),
        val hasMorePosts: Boolean = false,
        val isLoadingMorePosts: Boolean = false,
        val postPage: Int = 1
    ) : UserProfileUiState()
}