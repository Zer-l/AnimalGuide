package com.permissionx.animalguide.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale

object ImageCompressor {

    private const val MAX_SIZE_BYTES = 800 * 1024

    fun uriToBase64(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val scaled = scaleBitmap(originalBitmap, 1280)

        val outputStream = ByteArrayOutputStream()
        var quality = 90
        do {
            outputStream.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputStream.size() > MAX_SIZE_BYTES && quality > 10)

        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSide: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSide && height <= maxSide) return bitmap
        val scale = maxSide.toFloat() / maxOf(width, height)
        return bitmap.scale((width * scale).toInt(), (height * scale).toInt())
    }
}