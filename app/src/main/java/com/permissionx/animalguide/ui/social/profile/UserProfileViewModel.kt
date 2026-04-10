package com.permissionx.animalguide.ui.social.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val userSessionManager: UserSessionManager
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
            _state.value = UserProfileUiState.Loading

            val userResult = userRepository.getUserProfile(uid)
            userResult.onFailure {
                _state.value = UserProfileUiState.Error(it.message ?: "加载失败")
                return@launch
            }

            val user = userResult.getOrNull()!!

            val isFollowing = if (!isOwnProfile) {
                followRepository.isFollowing(uid).getOrNull() ?: false
            } else false

            _state.value = UserProfileUiState.Success(
                user = user,
                isFollowing = isFollowing
            )

            loadPosts(reset = true)
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
            if (!reset) {
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
            }
            result.onFailure {
                val current = _state.value as? UserProfileUiState.Success ?: return@launch
                _state.value = current.copy(isLoadingMorePosts = false)
            }
        }
    }
}