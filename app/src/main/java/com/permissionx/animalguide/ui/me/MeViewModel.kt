package com.permissionx.animalguide.ui.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.data.repository.PostUpdateEvent
import com.permissionx.animalguide.data.repository.UserRepository
import com.permissionx.animalguide.domain.usecase.social.auth.DeleteAccountUseCase
import com.permissionx.animalguide.domain.usecase.social.auth.LogoutUseCase
import com.permissionx.animalguide.domain.usecase.social.user.GetUserPostsUseCase
import com.permissionx.animalguide.domain.usecase.social.user.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DeleteAccountState {
    data object Idle : DeleteAccountState()
    data object Loading : DeleteAccountState()
    data object Success : DeleteAccountState()
    data class Error(val message: String) : DeleteAccountState()
}

@HiltViewModel
class MeViewModel @Inject constructor(
    private val userSessionManager: UserSessionManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val postRepository: PostRepository,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val userRepository: UserRepository,
    private val postUpdateEvent: PostUpdateEvent
) : ViewModel() {

    private val _state = MutableStateFlow<MeUiState>(MeUiState.Loading)
    val state = _state.asStateFlow()
    val currentUser = userSessionManager.currentUser
    private var currentPageMyPosts = 1
    private var currentPageCollects = 1

    init {
        load()
        viewModelScope.launch {
            postUpdateEvent.updates.collect { updatedPost ->
                val s = _state.value as? MeUiState.Success ?: return@collect
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

    fun load() {
        val currentUser = userSessionManager.currentUser.value
        if (currentUser == null) {
            _state.value = MeUiState.Guest
            return
        }

        val hasData = _state.value is MeUiState.Success

        viewModelScope.launch {
            // 已有数据时不重置为 Loading，静默刷新避免白屏
            if (!hasData) _state.value = MeUiState.Loading

            val profileResult = getUserProfileUseCase(currentUser.uid)
            profileResult.fold(
                onSuccess = { user ->
                    val current = _state.value as? MeUiState.Success
                    if (current != null) {
                        // 保留已有帖子列表，只更新用户信息
                        _state.value = current.copy(user = user)
                    } else {
                        _state.value = MeUiState.Success(user = user)
                        loadMyPosts(currentUser.uid)
                    }
                },
                onFailure = {
                    // 已有数据时静默失败，不破坏现有展示
                    if (!hasData) {
                        _state.value = MeUiState.Error(it.message ?: "加载失败，请重试")
                    }
                }
            )
        }
    }

    private fun loadMyPosts(uid: String) {
        viewModelScope.launch {
            currentPageMyPosts = 1

            // 先展示缓存，避免白屏等待
            val cached = postRepository.getCachedUserPosts(uid)
            if (cached.isNotEmpty()) {
                val s = _state.value as? MeUiState.Success ?: return@launch
                _state.value = s.copy(posts = cached, hasMorePosts = true, selectedTab = 0)
            }

            // 后台刷新网络数据
            val result = getUserPostsUseCase(uid, currentPageMyPosts)
            result.onSuccess { (posts, hasMore) ->
                val s = _state.value as? MeUiState.Success ?: return@onSuccess
                _state.value = s.copy(posts = posts, hasMorePosts = hasMore, selectedTab = 0)
                postRepository.cacheUserPosts(uid, posts)
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

    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState = _deleteAccountState.asStateFlow()

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteAccountState.value = DeleteAccountState.Loading
            val result = deleteAccountUseCase()
            _deleteAccountState.value = if (result.isSuccess) {
                DeleteAccountState.Success
            } else {
                DeleteAccountState.Error(result.exceptionOrNull()?.message ?: "注销失败，请稍后重试")
            }
        }
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = DeleteAccountState.Idle
    }

    fun toggleLike(post: com.permissionx.animalguide.domain.model.social.Post) {
        viewModelScope.launch {
            val result = postRepository.toggleLike(post)
            result.onSuccess { updatedPost ->
                val current = _state.value as? MeUiState.Success ?: return@onSuccess
                _state.value = current.copy(
                    posts = current.posts.map { if (it.id == updatedPost.id) updatedPost else it },
                    user = current.user.copy(
                        likeCount = if (updatedPost.isLiked) {
                            current.user.likeCount + 1
                        } else {
                            (current.user.likeCount - 1).coerceAtLeast(0)
                        }
                    )
                )
            }
        }
    }

    fun toggleCollect(post: com.permissionx.animalguide.domain.model.social.Post) {
        viewModelScope.launch {
            val result = postRepository.toggleCollect(post)
            result.onSuccess { updatedPost ->
                val current = _state.value as? MeUiState.Success ?: return@onSuccess
                if (current.selectedTab == 1 && !updatedPost.isCollected) {
                    // 收藏页取消收藏，从列表移除
                    _state.value = current.copy(
                        posts = current.posts.filter { it.id != updatedPost.id }
                    )
                } else {
                    _state.value = current.copy(
                        posts = current.posts.map { if (it.id == updatedPost.id) updatedPost else it }
                    )
                }
            }
        }
    }

    fun updateBackground(uri: android.net.Uri) {
        viewModelScope.launch {
            val s = _state.value as? MeUiState.Success ?: return@launch
            val result = userRepository.updateBackground(uri)
            result.onSuccess { url ->
                _state.value = s.copy(user = s.user.copy(backgroundUrl = url))
            }
        }
    }

    fun updateAvatar(uri: android.net.Uri) {
        viewModelScope.launch {
            val s = _state.value as? MeUiState.Success ?: return@launch
            val result = userRepository.updateAvatar(uri)
            result.onSuccess { url ->
                _state.value = s.copy(user = s.user.copy(avatarUrl = url))
            }
        }
    }
}