package com.permissionx.animalguide.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.yalantis.ucrop.UCrop
import java.io.File

enum class CropType {
    AVATAR,     // 圆形 1:1
    BACKGROUND  // 矩形 2:1
}

class CropImageContract : ActivityResultContract<CropImageInput, Uri?>() {

    override fun createIntent(context: Context, input: CropImageInput): Intent {
        val destinationFile = File(
            context.cacheDir,
            "cropped_${System.currentTimeMillis()}.jpg"
        )
        val destinationUri = Uri.fromFile(destinationFile)

        val options = UCrop.Options().apply {
            setCompressionQuality(85)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(false)
            if (input.cropType == CropType.AVATAR) {
                setCircleDimmedLayer(true)
            }
        }

        val uCrop = UCrop.of(input.sourceUri, destinationUri)
            .withOptions(options)

        when (input.cropType) {
            CropType.AVATAR -> uCrop.withAspectRatio(1f, 1f)
                .withMaxResultSize(512, 512)

            CropType.BACKGROUND -> uCrop.withAspectRatio(3f, 2f)
                .withMaxResultSize(1200, 800)
        }

        return uCrop.getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            UCrop.getOutput(intent)
        } else null
    }
}

data class CropImageInput(
    val sourceUri: Uri,
    val cropType: CropType
)