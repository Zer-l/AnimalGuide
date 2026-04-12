package com.permissionx.animalguide.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 用于跨屏幕通知 SocialScreen 切换到"最新"Tab 并刷新 */
@Singleton
class SocialNavigationEvent @Inject constructor() {
    private val _navigateToLatest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToLatest: SharedFlow<Unit> = _navigateToLatest.asSharedFlow()

    suspend fun emitNavigateToLatest() {
        _navigateToLatest.emit(Unit)
    }
}
