package com.permissionx.animalguide.ui.social.follow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.repository.FollowRepository
import com.permissionx.animalguide.data.repository.FollowUserItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowListUiState(
    val users: List<FollowUserItem> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
    val page: Int = 1,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FollowListUiState())
    val state: StateFlow<FollowListUiState> = _state.asStateFlow()

    private var uid: String = ""
    private var isFollowingMode: Boolean = true  // true=关注列表, false=粉丝列表

    fun load(uid: String, isFollowing: Boolean) {
        this.uid = uid
        this.isFollowingMode = isFollowing
        viewModelScope.launch {
            _state.value = FollowListUiState(isLoading = true)
            val result = if (isFollowing) {
                followRepository.getFollowingList(uid, pageNumber = 1)
            } else {
                followRepository.getFollowerList(uid, pageNumber = 1)
            }
            result.fold(
                onSuccess = { (users, hasMore) ->
                    _state.value = FollowListUiState(
                        users = users,
                        isLoading = false,
                        hasMore = hasMore,
                        page = 2
                    )
                },
                onFailure = {
                    _state.value = FollowListUiState(
                        isLoading = false,
                        error = it.message
                    )
                }
            )
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoadingMore || !s.hasMore) return

        viewModelScope.launch {
            _state.value = s.copy(isLoadingMore = true)
            val result = if (isFollowingMode) {
                followRepository.getFollowingList(uid, pageNumber = s.page)
            } else {
                followRepository.getFollowerList(uid, pageNumber = s.page)
            }
            result.fold(
                onSuccess = { (users, hasMore) ->
                    val current = _state.value
                    _state.value = current.copy(
                        users = current.users + users,
                        hasMore = hasMore,
                        page = current.page + 1,
                        isLoadingMore = false
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(isLoadingMore = false)
                }
            )
        }
    }
}