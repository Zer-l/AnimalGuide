package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.remote.cloudbase.FollowDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowRepository @Inject constructor(
    private val followDataSource: FollowDataSource,
    private val userDataSource: UserDataSource,
    private val userSessionManager: UserSessionManager
) {
    suspend fun isFollowing(toUid: String): Result<Boolean> {
        val fromUid = userSessionManager.currentUser.value?.uid
            ?: return Result.success(false)
        return followDataSource.isFollowing(fromUid, toUid)
    }

    suspend fun toggleFollow(toUid: String, isFollowing: Boolean): Result<Boolean> {
        val fromUid = userSessionManager.currentUser.value?.uid
            ?: return Result.failure(Exception("请先登录"))

        return if (isFollowing) {
            // 取消关注：followCount -1，被关注者的 followerCount -1
            val result = followDataSource.unfollow(fromUid, toUid)
            result.onSuccess {
                android.util.Log.d("FollowRepo", "取消关注成功，更新计数")
                try {
                    userDataSource.updateUserCount(fromUid, "followCount", -1)
                    userDataSource.updateUserCount(toUid, "followerCount", -1)
                } catch (e: Exception) {
                    android.util.Log.e("FollowRepo", "更新计数失败: ${e.message}")
                }
            }
            result
        } else {
            // 关注：followCount +1，被关注者的 followerCount +1
            val result = followDataSource.follow(fromUid, toUid)
            result.onSuccess {
                android.util.Log.d("FollowRepo", "关注成功，更新计数")
                try {
                    userDataSource.updateUserCount(fromUid, "followCount", 1)
                    userDataSource.updateUserCount(toUid, "followerCount", 1)
                } catch (e: Exception) {
                    android.util.Log.e("FollowRepo", "更新计数失败: ${e.message}")
                }
            }
            result
        }
    }
}