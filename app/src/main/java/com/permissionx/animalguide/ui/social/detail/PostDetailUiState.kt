package com.permissionx.animalguide.ui.social.detail

import com.permissionx.animalguide.domain.model.social.Comment
import com.permissionx.animalguide.domain.model.social.Post

data class PostDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val hasMoreComments: Boolean = false,
    val isLoading: Boolean = true,
    val isLoadingMoreComments: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val replyTo: Comment? = null,
    val expandedReplies: Set<String> = emptySet(),
    val repliesMap: Map<String, List<Comment>> = emptyMap(),
    val isFollowing: Boolean = false,
    val error: String? = null,
    /** 帖子内容来自缓存，评论等实时数据不可用 */
    val isOffline: Boolean = false
)