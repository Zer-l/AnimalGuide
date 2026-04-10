package com.permissionx.animalguide.ui.social.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.data.repository.PostUpdateEvent
import com.permissionx.animalguide.domain.model.social.Post
import com.permissionx.animalguide.domain.usecase.social.post.GetFeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getFeedUseCase: GetFeedUseCase,
    private val postRepository: PostRepository,
    private val postUpdateEvent: PostUpdateEvent
) : ViewModel() {

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state = _state.asStateFlow()

    private var currentPage = 1
    private var sortByHot = false
    private val pageSize = 10

    private var initialized = false

    init {
        viewModelScope.launch {
            postUpdateEvent.updates.collect { updatedPost ->
                val s = _state.value as? FeedUiState.Success ?: return@collect
                _state.value = s.copy(
                    posts = s.posts.map {
                        if (it.id == updatedPost.id) it.copy(
                            likeCount = updatedPost.likeCount,
                            commentCount = updatedPost.commentCount,
                            collectCount = updatedPost.collectCount,
                            isLiked = updatedPost.isLiked,
                            isCollected = updatedPost.isCollected
                        ) else it
                    }
                )
            }
        }
    }

    fun init(sortByHot: Boolean) {
        this.sortByHot = sortByHot
        if (!initialized) {
            initialized = true
            loadFirstPage()
        }
    }

    fun loadFirstPage() {
        viewModelScope.launch {
            _state.value = FeedUiState.Loading
            currentPage = 1
            val result = getFeedUseCase(
                pageSize = pageSize,
                pageNumber = currentPage,
                sortByHot = sortByHot
            )
            result.onSuccess { (posts, hasMore) ->
            }
            result.fold(
                onSuccess = { (posts, hasMore) ->
                    _state.value = if (posts.isEmpty()) {
                        FeedUiState.Empty
                    } else {
                        FeedUiState.Success(posts = posts, hasMore = hasMore)
                    }
                },
                onFailure = {
                    _state.value = FeedUiState.Error(it.message ?: "加载失败，请重试")
                }
            )
        }
    }

    fun loadMore() {
        val s = _state.value as? FeedUiState.Success ?: return
        if (!s.hasMore || s.isLoadingMore) return
        viewModelScope.launch {
            _state.value = s.copy(isLoadingMore = true)
            currentPage++
            val result = getFeedUseCase(
                pageSize = pageSize,
                pageNumber = currentPage,
                sortByHot = sortByHot
            )
            result.fold(
                onSuccess = { (newPosts, hasMore) ->
                    _state.value = s.copy(
                        posts = s.posts + newPosts,
                        hasMore = hasMore,
                        isLoadingMore = false
                    )
                },
                onFailure = {
                    currentPage--
                    _state.value = s.copy(isLoadingMore = false)
                }
            )
        }
    }

    fun refresh() = loadFirstPage()

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            val result = postRepository.toggleLike(post)
            result.onSuccess { updatedPost ->
                val s = _state.value as? FeedUiState.Success ?: return@onSuccess
                _state.value = s.copy(
                    posts = s.posts.map {
                        if (it.id == updatedPost.id) updatedPost else it
                    }
                )
            }
        }
    }

    fun toggleCollect(post: Post) {
        viewModelScope.launch {
            val result = postRepository.toggleCollect(post)
            result.onSuccess { updatedPost ->
                val s = _state.value as? FeedUiState.Success ?: return@onSuccess
                _state.value = s.copy(
                    posts = s.posts.map {
                        if (it.id == updatedPost.id) updatedPost else it
                    }
                )
            }
        }
    }

    fun pullToRefresh() {
        viewModelScope.launch {
            val s = _state.value as? FeedUiState.Success
            if (s != null) {
                _state.value = s.copy(isRefreshing = true)
            }
            currentPage = 1
            val result = getFeedUseCase(
                pageSize = pageSize,
                pageNumber = currentPage,
                sortByHot = sortByHot
            )
            result.fold(
                onSuccess = { (posts, hasMore) ->
                    _state.value = if (posts.isEmpty()) {
                        FeedUiState.Empty
                    } else {
                        FeedUiState.Success(
                            posts = posts,
                            hasMore = hasMore,
                            isRefreshing = false,
                            justRefreshed = true  // 标记
                        )
                    }
                },
                onFailure = {
                    val current = _state.value as? FeedUiState.Success
                    if (current != null) {
                        _state.value = current.copy(isRefreshing = false)
                    }
                }
            )
        }
    }

    fun clearJustRefreshed() {
        val s = _state.value as? FeedUiState.Success ?: return
        if (s.justRefreshed) {
            _state.value = s.copy(justRefreshed = false)
        }
    }
}