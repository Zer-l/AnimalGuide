package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.remote.cloudbase.LikeDataSource
import com.permissionx.animalguide.data.remote.cloudbase.PostDataSource
import com.permissionx.animalguide.data.remote.cloudbase.StorageDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.domain.model.social.Post
import com.permissionx.animalguide.domain.model.social.PostStatus
import com.permissionx.animalguide.domain.model.social.PostType
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.permissionx.animalguide.data.remote.cloudbase.CollectDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val postDataSource: PostDataSource,
    private val likeDataSource: LikeDataSource,
    private val collectDataSource: CollectDataSource,
    private val storageDataSource: StorageDataSource,
    private val userSessionManager: UserSessionManager,
    private val userRepository: UserRepository
) : ViewModel() {
    // 获取帖子列表
    suspend fun getPosts(
        pageSize: Int = 10,
        pageNumber: Int = 1,
        sortByHot: Boolean = false
    ): Result<Pair<List<Post>, Boolean>> {
        val result = postDataSource.getPosts(pageSize, pageNumber, sortByHot)
        return result.fold(
            onSuccess = { (records, hasMore) ->
                val uid = userSessionManager.currentUser.value?.uid
                val posts = records.map { it.toPost() }
                // 查询当前用户的点赞和收藏状态
                val postsWithStatus = if (uid != null) {
                    posts.map { post ->
                        val isLiked = likeDataSource.isLiked(uid, post.id, "POST")
                            .getOrNull() ?: false
                        val isCollected = collectDataSource.isCollected(uid, post.id)
                            .getOrNull() ?: false
                        post.copy(isLiked = isLiked, isCollected = isCollected)
                    }
                } else posts
                Result.success(Pair(postsWithStatus, hasMore))
            },
            onFailure = { Result.failure(it) }
        )
    }

    // 获取帖子详情
    suspend fun getPostById(postId: String): Result<Post> {
        val result = postDataSource.getPostById(postId)
        return result.fold(
            onSuccess = { map ->
                if (map != null) Result.success(map.toPost())
                else Result.failure(Exception("帖子不存在"))
            },
            onFailure = { Result.failure(it) }
        )
    }

    // 发帖
    suspend fun createPost(
        title: String,
        content: String,
        imageUris: List<Uri>,
        tags: List<String>,
        type: PostType,
        animalName: String = "",
        location: String = "",
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<String> {
        val user = userSessionManager.currentUser.value
            ?: return Result.failure(Exception("请先登录"))

        // 上传图片
        val imageUrls = mutableListOf<String>()
        imageUris.forEachIndexed { index, uri ->
            android.util.Log.d("PostRepo", "上传第${index + 1}张图片: $uri")
            val uploadResult = storageDataSource.uploadImage(
                uri = uri,
                path = "posts/${user.uid}/${System.currentTimeMillis()}_$index.jpg"
            )
            android.util.Log.d(
                "PostRepo",
                "上传结果: ${uploadResult.isSuccess}, 错误: ${uploadResult.exceptionOrNull()?.message}"
            )
            uploadResult.getOrNull()?.let { imageUrls.add(it) }
        }

        val coverUrl = imageUrls.firstOrNull() ?: ""

        val data = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "nickname" to user.nickname,
            "avatarUrl" to user.avatarUrl,
            "type" to type.name,
            "title" to title,
            "content" to content,
            "imageUrls" to imageUrls,
            "tags" to tags,
            "coverUrl" to coverUrl,
            "likeCount" to 0,
            "commentCount" to 0,
            "collectCount" to 0,
            "status" to "NORMAL"
        )

        android.util.Log.d("PostRepo", "开始创建帖子, uid: ${user.uid}")
        android.util.Log.d("PostRepo", "图片上传完成, urls: $imageUrls")
        android.util.Log.d("PostRepo", "发帖data: $data")

        if (animalName.isNotBlank()) data["animalName"] = animalName
        if (location.isNotBlank()) data["location"] = location
        latitude?.let { data["latitude"] = it }
        longitude?.let { data["longitude"] = it }

        val createResult = postDataSource.createPost(data)
        
        // 发帖成功后，更新用户的postCount
        createResult.onSuccess {
            android.util.Log.d("PostRepo", "帖子创建成功，更新用户postCount")
            try {
                userRepository.updateUserCount(user.uid, "postCount", 1)
                android.util.Log.d("PostRepo", "用户postCount已更新")
            } catch (e: Exception) {
                android.util.Log.e("PostRepo", "更新postCount失败: ${e.message}")
            }
        }
        
        return createResult
    }

    internal fun Map<String, Any>.toPost() = Post(
        id = this["_id"] as? String ?: "",
        uid = this["uid"] as? String ?: "",
        nickname = this["nickname"] as? String ?: "",
        avatarUrl = this["avatarUrl"] as? String ?: "",
        type = when (this["type"] as? String) {
            "ANIMAL_SHARE" -> PostType.ANIMAL_SHARE
            else -> PostType.ORIGINAL
        },
        animalName = this["animalName"] as? String ?: "",
        title = this["title"] as? String ?: "",
        content = this["content"] as? String ?: "",
        imageUrls = (this["imageUrls"] as? List<*>)
            ?.filterIsInstance<String>() ?: emptyList(),
        tags = (this["tags"] as? List<*>)
            ?.filterIsInstance<String>() ?: emptyList(),
        location = this["location"] as? String ?: "",
        latitude = this["latitude"] as? Double,
        longitude = this["longitude"] as? Double,
        likeCount = (this["likeCount"] as? Double)?.toInt() ?: 0,
        commentCount = (this["commentCount"] as? Double)?.toInt() ?: 0,
        collectCount = (this["collectCount"] as? Double)?.toInt() ?: 0,
        coverUrl = this["coverUrl"] as? String ?: "",
        status = when (this["status"] as? String) {
            "DELETED" -> PostStatus.DELETED
            "BLOCKED" -> PostStatus.BLOCKED
            else -> PostStatus.NORMAL
        },
        createdAt = (this["createdAt"] as? Double)?.toLong() ?: 0L
    )

    suspend fun toggleLike(post: Post): Result<Post> {
        val uid = userSessionManager.currentUser.value?.uid
            ?: return Result.failure(Exception("请先登录"))

        return if (post.isLiked) {
            val result = likeDataSource.unlike(uid, post.id, "POST")
            result.fold(
                onSuccess = {
                    val newCount = (post.likeCount - 1).coerceAtLeast(0)
                    postDataSource.updatePostCount(post.id, "likeCount", newCount)
                    // 更新帖子作者的likeCount -1
                    android.util.Log.d("PostRepo", "取消点赞成功，更新作者likeCount")
                    try {
                        userRepository.updateUserCount(post.uid, "likeCount", -1)
                    } catch (e: Exception) {
                        android.util.Log.e("PostRepo", "更新作者likeCount失败: ${e.message}")
                    }
                    Result.success(post.copy(isLiked = false, likeCount = newCount))
                },
                onFailure = { Result.failure(it) }
            )
        } else {
            val result = likeDataSource.like(uid, post.id, "POST")
            result.fold(
                onSuccess = {
                    val newCount = post.likeCount + 1
                    postDataSource.updatePostCount(post.id, "likeCount", newCount)
                    // 更新帖子作者的likeCount +1
                    android.util.Log.d("PostRepo", "点赞成功，更新作者likeCount")
                    try {
                        userRepository.updateUserCount(post.uid, "likeCount", 1)
                    } catch (e: Exception) {
                        android.util.Log.e("PostRepo", "更新作者likeCount失败: ${e.message}")
                    }
                    Result.success(post.copy(isLiked = true, likeCount = newCount))
                },
                onFailure = { Result.failure(it) }
            )
        }
    }

    suspend fun toggleCollect(post: Post): Result<Post> {
        val uid = userSessionManager.currentUser.value?.uid
            ?: return Result.failure(Exception("请先登录"))

        return if (post.isCollected) {
            val result = collectDataSource.uncollect(uid, post.id)
            result.fold(
                onSuccess = {
                    val newCount = (post.collectCount - 1).coerceAtLeast(0)
                    postDataSource.updatePostCount(post.id, "collectCount", newCount)
                    Result.success(post.copy(isCollected = false, collectCount = newCount))
                },
                onFailure = { Result.failure(it) }
            )
        } else {
            val result = collectDataSource.collect(uid, post.id)
            result.fold(
                onSuccess = {
                    val newCount = post.collectCount + 1
                    postDataSource.updatePostCount(post.id, "collectCount", newCount)
                    Result.success(post.copy(isCollected = true, collectCount = newCount))
                },
                onFailure = { Result.failure(it) }
            )
        }
    }

    // 获取帖子收藏状态
    suspend fun getPostWithStatus(postId: String): Result<Post> {
        val postResult = getPostById(postId)
        postResult.onFailure { return Result.failure(it) }
        val post = postResult.getOrNull()!!
        val uid = userSessionManager.currentUser.value?.uid ?: return Result.success(post)

        val isLiked = likeDataSource.isLiked(uid, postId, "POST").getOrNull() ?: false
        val isCollected = collectDataSource.isCollected(uid, postId).getOrNull() ?: false
        return Result.success(post.copy(isLiked = isLiked, isCollected = isCollected))
    }

    // 删除帖子
    suspend fun deletePost(postId: String): Result<Boolean> {
        // 先获取帖子信息，以便删除后更新作者的postCount
        val postResult = getPostById(postId)
        if (postResult.isFailure) {
            return Result.failure(Exception("获取帖子信息失败"))
        }
        
        val post = postResult.getOrNull() ?: return Result.failure(Exception("帖子不存在"))
        
        // 删除帖子
        val deleteResult = postDataSource.deletePost(postId)
        
        deleteResult.onSuccess {
            // 删除成功后，更新帖子作者的postCount -1
            android.util.Log.d("PostRepo", "帖子删除成功，更新作者postCount")
            try {
                userRepository.updateUserCount(post.uid, "postCount", -1)
                android.util.Log.d("PostRepo", "作者postCount已更新")
            } catch (e: Exception) {
                android.util.Log.e("PostRepo", "更新postCount失败: ${e.message}")
            }
        }
        
        return deleteResult
    }

    suspend fun getUserPosts(
        uid: String,
        pageNumber: Int = 1,
        pageSize: Int = 10
    ): Result<Pair<List<Post>, Boolean>> {
        val result = postDataSource.getPosts(
            pageSize = pageSize,
            pageNumber = pageNumber,
            sortByHot = false,
            filterUid = uid
        )
        return result.fold(
            onSuccess = { (records, hasMore) ->
                Result.success(Pair(records.map { it.toPost() }, hasMore))
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun getUserCollects(
        uid: String,
        pageNumber: Int = 1,
        pageSize: Int = 10
    ): Result<Pair<List<Post>, Boolean>> {
        // 先获取用户的收藏帖子ID列表
        val collectResult = collectDataSource.getUserCollectPostIds(uid, pageSize, pageNumber)
        return collectResult.fold(
            onSuccess = { (postIds, hasMore) ->
                if (postIds.isEmpty()) {
                    Result.success(Pair(emptyList(), hasMore))
                } else {
                    // 根据ID列表获取完整的帖子信息
                    val posts = postIds.mapNotNull { postId ->
                        getPostById(postId).getOrNull()
                    }
                    // 标记为已收藏
                    val collectedPosts = posts.map { it.copy(isCollected = true) }
                     Result.success(Pair(collectedPosts, hasMore))
                 }
             },
             onFailure = { Result.failure(it) }
         )
     }
}
