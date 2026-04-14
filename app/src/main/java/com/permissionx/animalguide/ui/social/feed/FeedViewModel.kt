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
            currentPage = 1

            // Step 1：立即展示本地缓存（如有），避免白屏
            val cached = postRepository.getCachedPosts(sortByHot)
            if (cached.isNotEmpty()) {
                _state.value = FeedUiState.Success(
                    posts = cached,
                    hasMore = true,
                    isOffline = false
                )
            } else {
                _state.value = FeedUiState.Loading
            }

            // Step 2：从网络加载最新数据
            val result = getFeedUseCase(
                pageSize = pageSize,
                pageNumber = currentPage,
                sortByHot = sortByHot
            )
            result.fold(
                onSuccess = { (posts, hasMore) ->
                    postRepository.cacheFirstPage(posts, sortByHot)
                    _state.value = if (posts.isEmpty()) {
                        FeedUiState.Empty
                    } else {
                        FeedUiState.Success(posts = posts, hasMore = hasMore, isOffline = false)
                    }
                },
                onFailure = {
                    val current = _state.value as? FeedUiState.Success
                    if (current != null) {
                        // 保持缓存展示，标记为离线
                        _state.value = current.copy(isOffline = true)
                    } else {
                        _state.value = FeedUiState.Error(it.message ?: "加载失败，请重试")
                    }
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
                    postRepository.cacheFirstPage(posts, sortByHot)
                    _state.value = if (posts.isEmpty()) {
                        FeedUiState.Empty
                    } else {
                        FeedUiState.Success(
                            posts = posts,
                            hasMore = hasMore,
                            isRefreshing = false,
                            justRefreshed = true,
                            isOffline = false
                        )
                    }
                },
                onFailure = {
                    val current = _state.value as? FeedUiState.Success
                    if (current != null) {
                        _state.value = current.copy(isRefreshing = false, isOffline = true)
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