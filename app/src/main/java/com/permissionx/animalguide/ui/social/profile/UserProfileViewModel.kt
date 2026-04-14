package com.permissionx.animalguide.ui.social.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.local.CachedUserDao
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.data.repository.FollowRepository
import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository,
    private val userSessionManager: UserSessionManager,
    private val cachedUserDao: CachedUserDao
) : ViewModel() {

    private val _state = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    private var uid: String = ""

    val isOwnProfile: Boolean
        get() = uid == userSessionManager.currentUser.value?.uid

    val isLoggedIn: Boolean get() = userSessionManager.isLoggedIn

    fun load(uid: String) {
        this.uid = uid
        viewModelScope.launch {
            // Step 1：先从缓存取用户数据，立即展示，避免白屏等待
            val cached = cachedUserDao.getUserByUid(uid)
            if (cached != null) {
                _state.value = UserProfileUiState.Success(
                    user = cached.toUser(),
                    isFollowing = false  // 关注状态仍需网络，下面会更新
                )
            } else {
                _state.value = UserProfileUiState.Loading
            }

            // Step 2：从网络刷新完整数据
            val userResult = userRepository.getUserProfile(uid)
            userResult.onFailure {
                if (_state.value !is UserProfileUiState.Success) {
                    _state.value = UserProfileUiState.Error(it.message ?: "加载失败")
                }
                return@launch
            }

            val user = userResult.getOrNull()!!

            val isFollowing = if (!isOwnProfile) {
                followRepository.isFollowing(uid).getOrNull() ?: false
            } else false

            val current = _state.value as? UserProfileUiState.Success
            if (current != null) {
                // 保留已加载的帖子列表，只更新用户信息和关注状态
                _state.value = current.copy(user = user, isFollowing = isFollowing)
            } else {
                _state.value = UserProfileUiState.Success(user = user, isFollowing = isFollowing)
            }

            // 仅首次（无帖子数据时）加载帖子
            val s = _state.value as? UserProfileUiState.Success ?: return@launch
            if (s.posts.isEmpty()) loadPosts(reset = true)
        }
    }

    fun toggleFollow() {
        val s = _state.value as? UserProfileUiState.Success ?: return
        viewModelScope.launch {
            _state.value = s.copy(isFollowLoading = true)
            val result = followRepository.toggleFollow(uid, s.isFollowing)
            result.onSuccess {
                val newFollowerCount = if (s.isFollowing) {
                    (s.user.followerCount - 1).coerceAtLeast(0)
                } else {
                    s.user.followerCount + 1
                }
                _state.value = s.copy(
                    isFollowing = !s.isFollowing,
                    isFollowLoading = false,
                    user = s.user.copy(followerCount = newFollowerCount)
                )
            }
            result.onFailure {
                _state.value = s.copy(isFollowLoading = false)
            }
        }
    }

    fun loadMorePosts() {
        val s = _state.value as? UserProfileUiState.Success ?: return
        if (s.isLoadingMorePosts || !s.hasMorePosts) return
        loadPosts(reset = false)
    }

    private fun loadPosts(reset: Boolean) {
        val s = _state.value as? UserProfileUiState.Success ?: return
        val page = if (reset) 1 else s.postPage

        viewModelScope.launch {
            if (reset) {
                // 先展示缓存，避免白屏等待
                val cached = postRepository.getCachedUserPosts(uid)
                if (cached.isNotEmpty()) {
                    val current = _state.value as? UserProfileUiState.Success ?: return@launch
                    _state.value = current.copy(posts = cached, hasMorePosts = true)
                }
            } else {
                _state.value = s.copy(isLoadingMorePosts = true)
            }

            val result = postRepository.getUserPosts(uid, pageNumber = page)
            result.onSuccess { (posts, hasMore) ->
                val current = _state.value as? UserProfileUiState.Success ?: return@launch
                _state.value = current.copy(
                    posts = if (reset) posts else current.posts + posts,
                    hasMorePosts = hasMore,
                    postPage = page + 1,
                    isLoadingMorePosts = false
                )
                // 网络成功后更新缓存（仅第一页）
                if (reset) postRepository.cacheUserPosts(uid, posts)
            }
            result.onFailure {
                val current = _state.value as? UserProfileUiState.Success ?: return@launch
                _state.value = current.copy(isLoadingMorePosts = false)
            }
        }
    }
}