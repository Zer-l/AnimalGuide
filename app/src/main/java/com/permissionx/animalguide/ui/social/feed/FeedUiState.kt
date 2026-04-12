package com.permissionx.animalguide.ui.social.feed

import com.permissionx.animalguide.domain.model.social.Post

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(
        val posts: List<Post> = emptyList(),
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val isRefreshing: Boolean = false,
        val justRefreshed: Boolean = false,
        /** 正在显示本地缓存，网络请求尚未完成或已失败 */
        val isOffline: Boolean = false
    ) : FeedUiState()

    object Empty : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}