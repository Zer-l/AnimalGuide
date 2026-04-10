package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.remote.cloudbase.FollowDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import javax.inject.Inject
import javax.inject.Singleton

data class FollowUserItem(
    val uid: String,
    val nickname: String,
    val avatarUrl: String,
    val bio: String
)

@Singleton
class FollowRepository @Inject constructor(
    private val followDataSource: FollowDataSource,
    private val userDataSource: UserDataSource,
    private val userSessionManager: UserSessionManager
) {
    // 防止并发关注/取关
    private val togglingFollowIds = mutableSetOf<String>()

    suspend fun isFollowing(toUid: String): Result<Boolean> {
        val fromUid = userSessionManager.currentUser.value?.uid
            ?: return Result.success(false)
        return followDataSource.isFollowing(fromUid, toUid)
    }

    suspend fun toggleFollow(toUid: String, isFollowing: Boolean): Result<Boolean> {
        if (togglingFollowIds.contains(toUid)) return Result.failure(Exception("操作中"))
        togglingFollowIds.add(toUid)

        try {
            val fromUid = userSessionManager.currentUser.value?.uid
                ?: return Result.failure(Exception("请先登录"))

            return if (isFollowing) {
                val result = followDataSource.unfollow(fromUid, toUid)
                result.onSuccess {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        try {
                            userDataSource.updateUserCount(fromUid, "followCount", -1)
                            userDataSource.updateUserCount(toUid, "followerCount", -1)
                        } catch (e: Exception) {
                        }
                    }
                }
                result
            } else {
                val result = followDataSource.follow(fromUid, toUid)
                result.onSuccess {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        try {
                            userDataSource.updateUserCount(fromUid, "followCount", 1)
                            userDataSource.updateUserCount(toUid, "followerCount", 1)
                        } catch (e: Exception) {
                        }
                    }
                }
                result
            }
        } finally {
            togglingFollowIds.remove(toUid)
        }
    }

    // 获取关注列表（带用户信息）
    suspend fun getFollowingList(
        uid: String,
        pageNumber: Int = 1
    ): Result<Pair<List<FollowUserItem>, Boolean>> {
        val result = followDataSource.getFollowingList(uid, pageNumber = pageNumber)
        return result.fold(
            onSuccess = { (records, hasMore) ->
                val users = records.mapNotNull { record ->
                    val toUid = record["toUid"] as? String ?: return@mapNotNull null
                    val userResult = userDataSource.getUserByUid(toUid)
                    userResult.getOrNull()?.let { user ->
                        FollowUserItem(
                            uid = toUid,
                            nickname = user["nickname"] as? String ?: "",
                            avatarUrl = user["avatarUrl"] as? String ?: "",
                            bio = user["bio"] as? String ?: ""
                        )
                    }
                }
                Result.success(Pair(users, hasMore))
            },
            onFailure = { Result.failure(it) }
        )
    }

    // 获取粉丝列表（带用户信息）
    suspend fun getFollowerList(
        uid: String,
        pageNumber: Int = 1
    ): Result<Pair<List<FollowUserItem>, Boolean>> {
        val result = followDataSource.getFollowerList(uid, pageNumber = pageNumber)
        return result.fold(
            onSuccess = { (records, hasMore) ->
                val users = records.mapNotNull { record ->
                    val fromUid = record["fromUid"] as? String ?: return@mapNotNull null
                    val userResult = userDataSource.getUserByUid(fromUid)
                    userResult.getOrNull()?.let { user ->
                        FollowUserItem(
                            uid = fromUid,
                            nickname = user["nickname"] as? String ?: "",
                            avatarUrl = user["avatarUrl"] as? String ?: "",
                            bio = user["bio"] as? String ?: ""
                        )
                    }
                }
                Result.success(Pair(users, hasMore))
            },
            onFailure = { Result.failure(it) }
        )
    }
}