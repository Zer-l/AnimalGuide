package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.domain.model.social.Post
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostUpdateEvent @Inject constructor() {
    private val _updates = MutableSharedFlow<Post>(extraBufferCapacity = 10)
    val updates: SharedFlow<Post> = _updates.asSharedFlow()

    suspend fun emit(post: Post) {
        _updates.emit(post)
    }
}