package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.remote.cloudbase.CollectDataSource
import com.permissionx.animalguide.data.remote.cloudbase.LikeDataSource
import com.permissionx.animalguide.data.remote.cloudbase.SearchDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.domain.model.social.Post
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


data class UserSearchResult(
    val uid: String,
    val nickname: String,
    val avatarUrl: String,
    val bio: String
)

@Singleton
class SearchRepository @Inject constructor(
    private val searchDataSource: SearchDataSource,
    private val postRepository: PostRepository,
    private val userSessionManager: UserSessionManager,
    private val likeDataSource: LikeDataSource,
    private val collectDataSource: CollectDataSource
) {
    suspend fun searchPosts(
        keyword: String,
        pageNumber: Int = 1,
        pageSize: Int = 10
    ): Result<Pair<List<Post>, Boolean>> {
        val result = searchDataSource.searchPosts(keyword, pageSize, pageNumber)
        return result.fold(
            onSuccess = { (records, hasMore) ->
                val posts = records.map { with(postRepository) { it.toPost() } }
                val uid = userSessionManager.currentUser.value?.uid
                val postsWithStatus = if (uid != null) {
                    coroutineScope {
                        posts.map { post ->
                            async {
                                val isLiked = likeDataSource.isLiked(uid, post.id, "POST")
                                    .getOrNull() ?: false
                                val isCollected = collectDataSource.isCollected(uid, post.id)
                                    .getOrNull() ?: false
                                post.copy(isLiked = isLiked, isCollected = isCollected)
                            }
                        }.map { it.await() }
                    }
                } else posts
                Result.success(Pair(postsWithStatus, hasMore))
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun searchUsers(
        keyword: String,
        pageNumber: Int = 1,
        pageSize: Int = 20
    ): Result<Pair<List<UserSearchResult>, Boolean>> {
        val result = searchDataSource.searchUsers(keyword, pageSize, pageNumber)
        return result.fold(
            onSuccess = { (records, hasMore) ->
                val users = records.map { it.toUserSearchResult() }
                Result.success(Pair(users, hasMore))
            },
            onFailure = { Result.failure(it) }
        )
    }

    private fun Map<String, Any>.toUserSearchResult() = UserSearchResult(
        uid = this["_openid"] as? String ?: "",
        nickname = this["nickname"] as? String ?: "",
        avatarUrl = this["avatarUrl"] as? String ?: "",
        bio = this["bio"] as? String ?: ""
    )
}