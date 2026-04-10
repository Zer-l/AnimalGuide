package com.permissionx.animalguide.ui.social.search

import com.permissionx.animalguide.data.repository.UserSearchResult
import com.permissionx.animalguide.domain.model.social.Post

data class SearchUiState(
    val query: String = "",
    val selectedTab: Int = 0,  // 0=帖子, 1=用户
    val posts: List<Post> = emptyList(),
    val users: List<UserSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val hasMorePosts: Boolean = false,
    val hasMoreUsers: Boolean = false,
    val postPage: Int = 1,
    val userPage: Int = 1,
    val error: String? = null
)