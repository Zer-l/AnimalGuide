package com.permissionx.animalguide.data.remote.cloudbase

import android.content.Context
import android.net.Uri
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class StorageDataSource @Inject constructor(
    private val client: CloudBaseClient,
    @ApplicationContext private val context: Context
) {
    private val httpClient = OkHttpClient()

    suspend fun uploadImage(uri: Uri, path: String): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }
            val bytes = inputStream.readBytes()
            inputStream.close()

            val uploadInfoResult = client.request<List<Map<String, Any>>>(
                method = "POST",
                path = "/v1/storages/get-objects-upload-info",
                body = listOf(mapOf("objectId" to path)),
                typeToken = object : TypeToken<List<Map<String, Any>>>() {}
            )

            uploadInfoResult.onFailure {
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }

            val infoList = uploadInfoResult.getOrNull()

            if (infoList == null) {
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }

            val info = infoList.firstOrNull()

            if (info == null) {
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }

            if (info.containsKey("code")) {
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }

            val uploadUrl = info["uploadUrl"] as? String

            if (uploadUrl == null) {
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }

            val authorization = info["authorization"] as? String ?: ""
            val token = info["token"] as? String ?: ""
            val cloudObjectMeta = info["cloudObjectMeta"] as? String ?: ""
            val downloadUrl = info["downloadUrl"] as? String ?: ""

            val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .header("Authorization", authorization)
                .header("X-Cos-Security-Token", token)
                .header("X-Cos-Meta-Fileid", cloudObjectMeta)
                .build()

            // COS 上传部分改为
            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful || response.code == 204) {
                Result.success(downloadUrl)
            } else {
                Result.failure(Exception("图片上传失败，请稍后重试"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("图片上传失败，请稍后重试"))
        }
    }
}