package com.permissionx.animalguide.ui.social.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.remote.cloudbase.LikeDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.data.repository.CommentRepository
import com.permissionx.animalguide.data.repository.FollowRepository
import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.domain.model.social.Comment
import com.permissionx.animalguide.domain.usecase.social.comment.GetCommentsUseCase
import com.permissionx.animalguide.domain.usecase.social.comment.PublishCommentUseCase
import com.permissionx.animalguide.domain.usecase.social.post.DeletePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val followRepository: FollowRepository,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val publishCommentUseCase: PublishCommentUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(PostDetailUiState())
    val state = _state.asStateFlow()

    val currentUserId get() = userSessionManager.currentUser.value?.uid

    private var currentCommentPage = 1

    fun loadPost(postId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val postResult = postRepository.getPostWithStatus(postId)
            postResult.onFailure {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = it.message ?: "加载失败"
                )
                return@launch
            }

            val post = postResult.getOrNull()!!
            _state.value = _state.value.copy(post = post)

            // 加载关注状态
            if (post.uid != currentUserId) {
                val isFollowing = followRepository.isFollowing(post.uid).getOrNull() ?: false
                _state.value = _state.value.copy(isFollowing = isFollowing)
            }

            loadComments(postId)
        }
    }

    private fun loadComments(postId: String) {
        viewModelScope.launch {
            currentCommentPage = 1
            val result = getCommentsUseCase(postId, currentCommentPage)
            result.fold(
                onSuccess = { (comments, hasMore) ->
                    _state.value = _state.value.copy(
                        comments = comments,
                        hasMoreComments = hasMore,
                        isLoading = false
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(isLoading = false)
                }
            )
        }
    }

    fun loadMoreComments() {
        val s = _state.value
        if (!s.hasMoreComments || s.isLoadingMoreComments) return
        val postId = s.post?.id ?: return

        viewModelScope.launch {
            _state.value = s.copy(isLoadingMoreComments = true)
            currentCommentPage++
            val result = getCommentsUseCase(postId, currentCommentPage)
            result.fold(
                onSuccess = { (newComments, hasMore) ->
                    _state.value = _state.value.copy(
                        comments = _state.value.comments + newComments,
                        hasMoreComments = hasMore,
                        isLoadingMoreComments = false
                    )
                },
                onFailure = {
                    currentCommentPage--
                    _state.value = _state.value.copy(isLoadingMoreComments = false)
                }
            )
        }
    }

    fun toggleLike() {
        val post = _state.value.post ?: return
        viewModelScope.launch {
            val result = postRepository.toggleLike(post)
            result.onSuccess { updatedPost ->
                _state.value = _state.value.copy(post = updatedPost)
            }
        }
    }

    fun toggleCollect() {
        val post = _state.value.post ?: return
        viewModelScope.launch {
            val result = postRepository.toggleCollect(post)
            result.onSuccess { updatedPost ->
                _state.value = _state.value.copy(post = updatedPost)
            }
        }
    }

    fun setReplyTo(comment: Comment?) {
        _state.value = _state.value.copy(replyTo = comment)
    }

    fun submitComment(content: String) {
        val post = _state.value.post ?: return
        val replyTo = _state.value.replyTo

        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmittingComment = true)
            val result = publishCommentUseCase(
                postId = post.id,
                content = content,
                parentId = replyTo?.id,
                replyToUid = replyTo?.uid,
                replyToNickname = replyTo?.nickname
            )
            result.fold(
                onSuccess = { newComment ->
                    if (replyTo == null) {
                        // 一级评论，添加到列表头部
                        _state.value = _state.value.copy(
                            comments = listOf(newComment) + _state.value.comments,
                            replyTo = null,
                            isSubmittingComment = false,
                            post = _state.value.post?.copy(
                                commentCount = (_state.value.post?.commentCount ?: 0) + 1
                            )
                        )
                    } else {
                        // 回复，添加到对应评论的回复列表
                        val updatedReplies = _state.value.repliesMap.toMutableMap()
                        val currentReplies = updatedReplies[replyTo.id] ?: emptyList()
                        updatedReplies[replyTo.id] = currentReplies + newComment
                        _state.value = _state.value.copy(
                            repliesMap = updatedReplies,
                            replyTo = null,
                            isSubmittingComment = false,
                            post = _state.value.post?.copy(
                                commentCount = (_state.value.post?.commentCount ?: 0) + 1
                            )
                        )
                    }
                },
                onFailure = {
                    _state.value = _state.value.copy(isSubmittingComment = false)
                }
            )
        }
    }

    fun toggleReplies(commentId: String) {
        val s = _state.value
        if (s.expandedReplies.contains(commentId)) {
            _state.value = s.copy(
                expandedReplies = s.expandedReplies - commentId
            )
        } else {
            viewModelScope.launch {
                val result = commentRepository.getReplies(commentId)
                result.onSuccess { replies ->
                    val updatedReplies = s.repliesMap.toMutableMap()
                    updatedReplies[commentId] = replies
                    _state.value = _state.value.copy(
                        expandedReplies = _state.value.expandedReplies + commentId,
                        repliesMap = updatedReplies
                    )
                }
            }
        }
    }

    fun deletePost(onDeleted: () -> Unit) {
        val postId = _state.value.post?.id ?: return
        viewModelScope.launch {
            val result = deletePostUseCase(postId)
            result.onSuccess { onDeleted() }
        }
    }

    fun deleteComment(commentId: String) {
        val postId = _state.value.post?.id ?: return
        viewModelScope.launch {
            val result = commentRepository.deleteComment(commentId, postId)
            result.onSuccess {
                _state.value = _state.value.copy(
                    comments = _state.value.comments.filter { it.id != commentId },
                    post = _state.value.post?.copy(
                        commentCount = (_state.value.post?.commentCount ?: 1) - 1
                    )
                )
            }
        }
    }

    fun toggleFollow() {
        val post = _state.value.post ?: return
        val isFollowing = _state.value.isFollowing
        viewModelScope.launch {
            val result = followRepository.toggleFollow(post.uid, isFollowing)
            result.onSuccess {
                _state.value = _state.value.copy(isFollowing = !isFollowing)
            }
        }
    }

    fun likeComment(comment: Comment) {
        viewModelScope.launch {
            val result = commentRepository.toggleLikeComment(comment)
            result.onSuccess { updatedComment ->
                val updatedComments = _state.value.comments.map {
                    if (it.id == updatedComment.id) updatedComment else it
                }
                _state.value = _state.value.copy(comments = updatedComments)
            }
        }
    }
}