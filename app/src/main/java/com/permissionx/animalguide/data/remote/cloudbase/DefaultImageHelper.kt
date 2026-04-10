package com.permissionx.animalguide.data.remote.cloudbase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.permissionx.animalguide.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultImageHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val defaultAvatars = listOf(
        R.drawable.default_avatar_01,
        R.drawable.default_avatar_02,
        R.drawable.default_avatar_03,
        R.drawable.default_avatar_04,
        R.drawable.default_avatar_05,
        R.drawable.default_avatar_06,
        R.drawable.default_avatar_07,
        R.drawable.default_avatar_08,
        R.drawable.default_avatar_09,
        R.drawable.default_avatar_10,
        R.drawable.default_avatar_11,
        R.drawable.default_avatar_12,
    )

    private val defaultBackgrounds = listOf(
        R.drawable.default_bg_01,
        R.drawable.default_bg_02,
        R.drawable.default_bg_03,
        R.drawable.default_bg_04,
        R.drawable.default_bg_05,
        R.drawable.default_bg_06,
        R.drawable.default_bg_07,
        R.drawable.default_bg_08,
    )

    /**
     * 随机获取一个默认头像的 Uri
     */
    fun getRandomAvatarUri(): Uri {
        val resId = defaultAvatars.random()
        return resourceToUri(resId, "avatar")
    }

    /**
     * 随机获取一个默认背景图的 Uri
     */
    fun getRandomBackgroundUri(): Uri {
        val resId = defaultBackgrounds.random()
        return resourceToUri(resId, "background")
    }

    private fun resourceToUri(resId: Int, prefix: String): Uri {
        val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.webp")
        context.resources.openRawResource(resId).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}