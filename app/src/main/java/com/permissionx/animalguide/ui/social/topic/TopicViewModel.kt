package com.permissionx.animalguide.ui.social.topic

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.data.repository.PostUpdateEvent
import com.permissionx.animalguide.domain.model.social.Post
import com.permissionx.animalguide.ui.social.feed.FeedUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val postRepository: PostRepository,
    private val postUpdateEvent: PostUpdateEvent
) : ViewModel() {

    val tag: String = Uri.decode(savedStateHandle.get<String>("tag") ?: "")

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state = _state.asStateFlow()

    // 0 = 推荐（热度），1 = 最新
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private var currentPage = 1
    private val pageSize = 10

    init {
        loadFirstPage()
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

    fun selectTab(tab: Int) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
        loadFirstPage()
    }

    fun loadFirstPage() {
        viewModelScope.launch {
            currentPage = 1
            _state.value = FeedUiState.Loading
            postRepository.getPostsByTag(
                tag = tag,
                pageSize = pageSize,
                pageNumber = currentPage,
                sortByHot = _selectedTab.value == 0
            ).fold(
                onSuccess = { (posts, hasMore) ->
                    _state.value = if (posts.isEmpty()) FeedUiState.Empty
                    else FeedUiState.Success(posts = posts, hasMore = hasMore)
                },
                onFailure = {
                    _state.value = FeedUiState.Error(it.message ?: "加载失败")
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
            postRepository.getPostsByTag(
                tag = tag,
                pageSize = pageSize,
                pageNumber = currentPage,
                sortByHot = _selectedTab.value == 0
            ).fold(
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

    fun pullToRefresh() {
        viewModelScope.launch {
            val s = _state.value as? FeedUiState.Success
            if (s != null) _state.value = s.copy(isRefreshing = true)
            currentPage = 1
            postRepository.getPostsByTag(
                tag = tag,
                pageSize = pageSize,
                pageNumber = currentPage,
                sortByHot = _selectedTab.value == 0
            ).fold(
                onSuccess = { (posts, hasMore) ->
                    _state.value = if (posts.isEmpty()) FeedUiState.Empty
                    else FeedUiState.Success(
                        posts = posts,
                        hasMore = hasMore,
                        isRefreshing = false,
                        justRefreshed = true
                    )
                },
                onFailure = {
                    val current = _state.value as? FeedUiState.Success
                    if (current != null) _state.value = current.copy(isRefreshing = false)
                }
            )
        }
    }

    fun clearJustRefreshed() {
        val s = _state.value as? FeedUiState.Success ?: return
        if (s.justRefreshed) _state.value = s.copy(justRefreshed = false)
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            postRepository.toggleLike(post).onSuccess { updatedPost ->
                val s = _state.value as? FeedUiState.Success ?: return@onSuccess
                _state.value = s.copy(
                    posts = s.posts.map { if (it.id == updatedPost.id) updatedPost else it }
                )
            }
        }
    }

    fun toggleCollect(post: Post) {
        viewModelScope.launch {
            postRepository.toggleCollect(post).onSuccess { updatedPost ->
                val s = _state.value as? FeedUiState.Success ?: return@onSuccess
                _state.value = s.copy(
                    posts = s.posts.map { if (it.id == updatedPost.id) updatedPost else it }
                )
            }
        }
    }
}
