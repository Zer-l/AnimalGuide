package com.permissionx.animalguide.ui.social.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.repository.PostUpdateEvent
import com.permissionx.animalguide.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val postUpdateEvent: PostUpdateEvent
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        // 500ms 防抖
        _queryFlow
            .debounce(500)
            .filter { it.isNotBlank() }
            .distinctUntilChanged()
            .onEach { keyword -> doSearch(keyword) }
            .launchIn(viewModelScope)
        // 监听帖子更新
        viewModelScope.launch {
            postUpdateEvent.updates.collect { updatedPost ->
                val s = _state.value
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

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        _queryFlow.value = query
        if (query.isBlank()) {
            _state.value = SearchUiState()
        }
    }

    fun onTabChange(tab: Int) {
        _state.value = _state.value.copy(selectedTab = tab)
        // 切换 tab 时，如果该 tab 还没搜过，触发搜索
        val s = _state.value
        if (tab == 0 && s.posts.isEmpty() && s.query.isNotBlank()) {
            searchPosts(s.query, reset = true)
        } else if (tab == 1 && s.users.isEmpty() && s.query.isNotBlank()) {
            searchUsers(s.query, reset = true)
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.query.isBlank()) return
        if (s.selectedTab == 0 && s.hasMorePosts) {
            searchPosts(s.query, reset = false)
        } else if (s.selectedTab == 1 && s.hasMoreUsers) {
            searchUsers(s.query, reset = false)
        }
    }

    private fun doSearch(keyword: String) {
        // 重置结果，搜索当前 tab
        _state.value = _state.value.copy(
            posts = emptyList(),
            users = emptyList(),
            postPage = 1,
            userPage = 1,
            error = null
        )
        if (_state.value.selectedTab == 0) {
            searchPosts(keyword, reset = true)
        } else {
            searchUsers(keyword, reset = true)
        }
    }

    private fun searchPosts(keyword: String, reset: Boolean) {
        viewModelScope.launch {
            val page = if (reset) 1 else _state.value.postPage
            _state.value = _state.value.copy(isLoading = true, error = null)

            searchRepository.searchPosts(keyword, pageNumber = page).fold(
                onSuccess = { (posts, hasMore) ->
                    _state.value = _state.value.copy(
                        posts = if (reset) posts else _state.value.posts + posts,
                        hasMorePosts = hasMore,
                        postPage = page + 1,
                        isLoading = false
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = it.message
                    )
                }
            )
        }
    }

    private fun searchUsers(keyword: String, reset: Boolean) {
        viewModelScope.launch {
            val page = if (reset) 1 else _state.value.userPage
            _state.value = _state.value.copy(isLoading = true, error = null)

            searchRepository.searchUsers(keyword, pageNumber = page).fold(
                onSuccess = { (users, hasMore) ->
                    _state.value = _state.value.copy(
                        users = if (reset) users else _state.value.users + users,
                        hasMoreUsers = hasMore,
                        userPage = page + 1,
                        isLoading = false
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = it.message
                    )
                }
            )
        }
    }

    fun refreshCurrentResults() {
        val keyword = _state.value.query
        if (keyword.isBlank()) return
        // 重新搜索当前 tab
        if (_state.value.selectedTab == 0) {
            searchPosts(keyword, reset = true)
        } else {
            searchUsers(keyword, reset = true)
        }
    }
}