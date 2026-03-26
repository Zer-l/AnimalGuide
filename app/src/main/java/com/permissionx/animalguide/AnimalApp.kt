package com.permissionx.animalguide

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AnimalApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 配置 Coil 图片加载器
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25) // 使用25%内存作为缓存
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(50 * 1024 * 1024) // 固定50MB而不是百分比
                        .build()
                }
                .crossfade(true)
                .build()
        )
    }
}