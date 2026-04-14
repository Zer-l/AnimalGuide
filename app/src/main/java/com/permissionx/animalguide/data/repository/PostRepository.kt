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
import com.permissionx.animalguide.data.local.CachedPostDao
import com.permissionx.animalguide.data.local.entity.toCacheEntity
import com.permissionx.animalguide.data.remote.cloudbase.CollectDataSource
import com.permissionx.animalguide.data.remote.cloudbase.CommentDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class PostRepository @Inject constructor(
    private val postDataSource: PostDataSource,
    private val likeDataSource: LikeDataSource,
    private val collectDataSource: CollectDataSource,
    private val storageDataSource: StorageDataSource,
    private val userSessionManager: UserSessionManager,
    private val userRepository: UserRepository,
    private val commentDataSource: CommentDataSource,
    private val cachedPostDao: CachedPostDao
) : ViewModel() {
    // 防止同一帖子并发点赞/收藏
    private val likingPostIds = mutableSetOf<String>()
    private val collectingPostIds = mutableSetOf<String>()

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

    // 获取话题帖子列表（按标签过滤）
    suspend fun getPostsByTag(
        tag: String,
        pageSize: Int = 10,
        pageNumber: Int = 1,
        sortByHot: Boolean = false
    ): Result<Pair<List<Post>, Boolean>> {
        val result = postDataSource.getPosts(pageSize, pageNumber, sortByHot, filterTag = tag)
        return result.fold(
            onSuccess = { (records, hasMore) ->
                val uid = userSessionManager.currentUser.value?.uid
                val posts = records.map { it.toPost() }
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

    // 获取帖子详情：网络失败时回退本地缓存
    suspend fun getPostById(postId: String): Result<Post> {
        val result = postDataSource.getPostById(postId)
        return result.fold(
            onSuccess = { map ->
                if (map != null) Result.success(map.toPost())
                else Result.failure(Exception("帖子不存在"))
            },
            onFailure = {
                val cached = cachedPostDao.getPostById(postId)
                if (cached != null) Result.success(cached.toPost())
                else Result.failure(it)
            }
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
            val uploadResult = storageDataSource.uploadImage(
                uri = uri,
                path = "posts/${user.uid}/${System.currentTimeMillis()}_$index.jpg"
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

        if (animalName.isNotBlank()) data["animalName"] = animalName
        if (location.isNotBlank()) data["location"] = location
        latitude?.let { data["latitude"] = it }
        longitude?.let { data["longitude"] = it }

        val createResult = postDataSource.createPost(data)

        // 发帖成功后，更新用户的postCount
        createResult.onSuccess {
            try {
                userRepository.updateUserCount(user.uid, "postCount", 1)
            } catch (e: Exception) {
            }
        }

        return createResult
    }

    fun Map<String, Any>.toPost() = Post(
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
        if (likingPostIds.contains(post.id)) return Result.failure(Exception("操作中"))
        likingPostIds.add(post.id)

        try {
            val uid = userSessionManager.currentUser.value?.uid
                ?: return Result.failure(Exception("请先登录"))

            return if (post.isLiked) {
                val result = likeDataSource.unlike(uid, post.id, "POST")
                result.fold(
                    onSuccess = {
                        val newCount = (post.likeCount - 1).coerceAtLeast(0)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            postDataSource.updatePostCount(post.id, "likeCount", newCount)
                            cachedPostDao.updateLikeState(post.id, false, newCount)
                            try {
                                userRepository.updateUserCount(post.uid, "likeCount", -1)
                            } catch (e: Exception) {
                            }
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
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            postDataSource.updatePostCount(post.id, "likeCount", newCount)
                            cachedPostDao.updateLikeState(post.id, true, newCount)
                            try {
                                userRepository.updateUserCount(post.uid, "likeCount", 1)
                            } catch (e: Exception) {
                            }
                        }
                        Result.success(post.copy(isLiked = true, likeCount = newCount))
                    },
                    onFailure = { Result.failure(it) }
                )
            }
        } finally {
            likingPostIds.remove(post.id)
        }
    }

    suspend fun toggleCollect(post: Post): Result<Post> {
        if (collectingPostIds.contains(post.id)) return Result.failure(Exception("操作中"))
        collectingPostIds.add(post.id)

        try {
            val uid = userSessionManager.currentUser.value?.uid
                ?: return Result.failure(Exception("请先登录"))

            return if (post.isCollected) {
                val result = collectDataSource.uncollect(uid, post.id)
                result.fold(
                    onSuccess = {
                        val newCount = (post.collectCount - 1).coerceAtLeast(0)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            postDataSource.updatePostCount(post.id, "collectCount", newCount)
                            cachedPostDao.updateCollectState(post.id, false, newCount)
                        }
                        Result.success(post.copy(isCollected = false, collectCount = newCount))
                    },
                    onFailure = { Result.failure(it) }
                )
            } else {
                val result = collectDataSource.collect(uid, post.id)
                result.fold(
                    onSuccess = {
                        val newCount = post.collectCount + 1
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            postDataSource.updatePostCount(post.id, "collectCount", newCount)
                            cachedPostDao.updateCollectState(post.id, true, newCount)
                        }
                        Result.success(post.copy(isCollected = true, collectCount = newCount))
                    },
                    onFailure = { Result.failure(it) }
                )
            }
        } finally {
            collectingPostIds.remove(post.id)
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
        val postResult = getPostById(postId)
        if (postResult.isFailure) {
            return Result.failure(Exception("获取帖子信息失败"))
        }

        val post = postResult.getOrNull() ?: return Result.failure(Exception("帖子不存在"))

        val deleteResult = postDataSource.deletePost(postId)

        deleteResult.onSuccess {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                try {
                    // 1. 更新作者计数
                    userRepository.updateUserCount(post.uid, "postCount", -1)
                    if (post.likeCount > 0) {
                        userRepository.updateUserCount(post.uid, "likeCount", -post.likeCount)
                    }

                    // 2. 删除帖子的所有点赞记录
                    likeDataSource.deleteAllLikes(postId, "POST")

                    // 3. 删除帖子的所有收藏记录
                    collectDataSource.deleteAllCollects(postId)

                    // 4. 删除帖子的所有评论（含子评论）及评论点赞
                    val commentsResult = commentDataSource.getComments(postId, pageSize = 100)
                    commentsResult.onSuccess { (records, _) ->
                        records.forEach { record ->
                            val commentId = record["_id"] as? String ?: return@forEach
                            // 删除子评论
                            val repliesResult = commentDataSource.getReplies(commentId)
                            repliesResult.onSuccess { replies ->
                                replies.forEach { reply ->
                                    val replyId = reply["_id"] as? String ?: return@forEach
                                    likeDataSource.deleteAllLikes(replyId, "COMMENT")
                                    commentDataSource.deleteComment(replyId)
                                }
                            }
                            // 删除主评论点赞和主评论本身
                            likeDataSource.deleteAllLikes(commentId, "COMMENT")
                            commentDataSource.deleteComment(commentId)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PostRepository", "删帖清理失败: ${e.message}")
                }
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
                val currentUid = userSessionManager.currentUser.value?.uid
                val posts = records.map { it.toPost() }
                val postsWithStatus = if (currentUid != null) {
                    coroutineScope {
                        posts.map { post ->
                            async {
                                val isLiked = likeDataSource.isLiked(currentUid, post.id, "POST")
                                    .getOrNull() ?: false
                                val isCollected = collectDataSource.isCollected(currentUid, post.id)
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

    // ── 离线缓存 ────────────────────────────────────────────────────────────

    /** 从本地缓存读取第一页帖子（不含点赞/收藏状态） */
    suspend fun getCachedPosts(sortByHot: Boolean): List<Post> {
        val sortType = if (sortByHot) "hot" else "latest"
        return cachedPostDao.getPostsBySortType(sortType).map { it.toPost() }
    }

    /** 将网络返回的第一页帖子写入缓存（先清旧再写新） */
    suspend fun cacheFirstPage(posts: List<Post>, sortByHot: Boolean) {
        val sortType = if (sortByHot) "hot" else "latest"
        cachedPostDao.deletePostsBySortType(sortType)
        cachedPostDao.insertAll(posts.mapIndexed { index, post -> post.toCacheEntity(sortType, index) })
    }

    /** 从本地缓存读取单条帖子（用于详情页缓存优先展示） */
    suspend fun getCachedPost(postId: String): Post? {
        return cachedPostDao.getPostById(postId)?.toPost()
    }

    /** 从本地缓存读取某用户的帖子列表 */
    suspend fun getCachedUserPosts(uid: String): List<Post> {
        return cachedPostDao.getPostsBySortType("user_$uid").map { it.toPost() }
    }

    /** 将用户帖子第一页写入缓存（先清旧再写新） */
    suspend fun cacheUserPosts(uid: String, posts: List<Post>) {
        val sortType = "user_$uid"
        cachedPostDao.deletePostsBySortType(sortType)
        cachedPostDao.insertAll(posts.mapIndexed { index, post -> post.toCacheEntity(sortType, index) })
    }

    // ────────────────────────────────────────────────────────────────────────

    suspend fun getUserCollects(
        uid: String,
        pageNumber: Int = 1,
        pageSize: Int = 10
    ): Result<Pair<List<Post>, Boolean>> {
        val collectResult = collectDataSource.getUserCollectPostIds(uid, pageSize, pageNumber)
        return collectResult.fold(
            onSuccess = { (postIds, hasMore) ->
                if (postIds.isEmpty()) {
                    Result.success(Pair(emptyList(), hasMore))
                } else {
                    val currentUid = userSessionManager.currentUser.value?.uid
                    coroutineScope {
                        val posts = postIds.map { postId ->
                            async {
                                getPostById(postId).getOrNull()
                            }
                        }.mapNotNull { it.await() }

                        val postsWithStatus = posts.map { post ->
                            async {
                                val isLiked = if (currentUid != null) {
                                    likeDataSource.isLiked(currentUid, post.id, "POST")
                                        .getOrNull() ?: false
                                } else false
                                post.copy(isLiked = isLiked, isCollected = true)
                            }
                        }.map { it.await() }

                        Result.success(Pair(postsWithStatus, hasMore))
                    }
                }
            },
            onFailure = { Result.failure(it) }
        )
    }
}
