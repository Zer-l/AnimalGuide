package com.permissionx.animalguide.ui.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.domain.usecase.social.auth.LogoutUseCase
import com.permissionx.animalguide.domain.usecase.social.user.GetUserPostsUseCase
import com.permissionx.animalguide.domain.usecase.social.user.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeViewModel @Inject constructor(
    private val userSessionManager: UserSessionManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val postRepository: PostRepository,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<MeUiState>(MeUiState.Loading)
    val state = _state.asStateFlow()
    val currentUser = userSessionManager.currentUser
    private var currentPageMyPosts = 1
    private var currentPageCollects = 1

    init {
        load()
    }

    fun load() {
        val currentUser = userSessionManager.currentUser.value
        if (currentUser == null) {
            _state.value = MeUiState.Guest
            return
        }

        android.util.Log.d("MeViewModel", "开始加载用户资料，uid: ${currentUser.uid}")
        
        viewModelScope.launch {
            _state.value = MeUiState.Loading

            val profileResult = getUserProfileUseCase(currentUser.uid)
            profileResult.fold(
                onSuccess = { user ->
                    android.util.Log.d("MeViewModel", "用户资料加载成功，postCount: ${user.postCount}, followCount: ${user.followCount}, followerCount: ${user.followerCount}")
                    _state.value = MeUiState.Success(user = user)
                    loadMyPosts(currentUser.uid)
                },
                onFailure = {
                    android.util.Log.e("MeViewModel", "加载用户资料失败: ${it.message}")
                    _state.value = MeUiState.Error(
                        it.message ?: "加载失败，请重试"
                    )
                }
            )
        }
    }

    private fun loadMyPosts(uid: String) {
        viewModelScope.launch {
            currentPageMyPosts = 1
            val result = getUserPostsUseCase(uid, currentPageMyPosts)
            result.onSuccess { (posts, hasMore) ->
                val s = _state.value as? MeUiState.Success ?: return@onSuccess
                _state.value = s.copy(posts = posts, hasMorePosts = hasMore, selectedTab = 0)
            }
        }
    }

    private fun loadCollects(uid: String) {
        viewModelScope.launch {
            currentPageCollects = 1
            // 获取用户收藏的帖子
            val result = postRepository.getUserCollects(uid, currentPageCollects)
            result.onSuccess { (posts, hasMore) ->
                val s = _state.value as? MeUiState.Success ?: return@onSuccess
                _state.value = s.copy(posts = posts, hasMorePosts = hasMore, selectedTab = 1)
            }
        }
    }

    fun loadMorePosts() {
        val s = _state.value as? MeUiState.Success ?: return
        if (!s.hasMorePosts || s.isLoadingMorePosts) return
        val uid = userSessionManager.currentUser.value?.uid ?: return

        viewModelScope.launch {
            _state.value = s.copy(isLoadingMorePosts = true)
            
            if (s.selectedTab == 0) {
                // 加载更多"我的帖子"
                currentPageMyPosts++
                val result = getUserPostsUseCase(uid, currentPageMyPosts)
                result.fold(
                    onSuccess = { (newPosts, hasMore) ->
                        val current = _state.value as? MeUiState.Success ?: return@fold
                        _state.value = current.copy(
                            posts = current.posts + newPosts,
                            hasMorePosts = hasMore,
                            isLoadingMorePosts = false
                        )
                    },
                    onFailure = {
                        currentPageMyPosts--
                        val current = _state.value as? MeUiState.Success ?: return@fold
                        _state.value = current.copy(isLoadingMorePosts = false)
                    }
                )
            } else {
                // 加载更多"收藏"
                currentPageCollects++
                val result = postRepository.getUserCollects(uid, currentPageCollects)
                result.fold(
                    onSuccess = { (newPosts, hasMore) ->
                        val current = _state.value as? MeUiState.Success ?: return@fold
                        _state.value = current.copy(
                            posts = current.posts + newPosts,
                            hasMorePosts = hasMore,
                            isLoadingMorePosts = false
                        )
                    },
                    onFailure = {
                        currentPageCollects--
                        val current = _state.value as? MeUiState.Success ?: return@fold
                        _state.value = current.copy(isLoadingMorePosts = false)
                    }
                )
            }
        }
    }

    fun selectTab(index: Int) {
        val uid = userSessionManager.currentUser.value?.uid ?: return
        
        // 切换tab时加载对应数据
        if (index == 0) {
            loadMyPosts(uid)
        } else if (index == 1) {
            loadCollects(uid)
        }
    }

    fun logout() = logoutUseCase()
}