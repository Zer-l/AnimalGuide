package com.permissionx.animalguide.ui.social.feed

import com.permissionx.animalguide.domain.model.social.Post

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(
        val posts: List<Post>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : FeedUiState()

    object Empty : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}