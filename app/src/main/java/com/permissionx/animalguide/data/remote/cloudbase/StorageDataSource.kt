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
        android.util.Log.d("StorageDS", "开始上传图片: $uri, path: $path")
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                android.util.Log.e("StorageDS", "无法读取图片")
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }
            val bytes = inputStream.readBytes()
            inputStream.close()
            android.util.Log.d("StorageDS", "图片读取成功, 大小: ${bytes.size}")

            val uploadInfoResult = client.request<List<Map<String, Any>>>(
                method = "POST",
                path = "/v1/storages/get-objects-upload-info",
                body = listOf(mapOf("objectId" to path)),
                typeToken = object : TypeToken<List<Map<String, Any>>>() {}
            )
            android.util.Log.d(
                "StorageDS",
                "获取上传信息结果: isSuccess=${uploadInfoResult.isSuccess}, error=${uploadInfoResult.exceptionOrNull()?.message}"
            )

            uploadInfoResult.onFailure {
                android.util.Log.e("StorageDS", "获取上传信息失败: ${it.message}")
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }

            val infoList = uploadInfoResult.getOrNull()
            android.util.Log.d("StorageDS", "infoList: $infoList")

            if (infoList == null) {
                android.util.Log.e("StorageDS", "infoList为null")
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }

            val info = infoList.firstOrNull()
            android.util.Log.d("StorageDS", "info: $info")

            if (info == null) {
                android.util.Log.e("StorageDS", "info为null")
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }

            if (info.containsKey("code")) {
                android.util.Log.e(
                    "StorageDS",
                    "info包含错误code: ${info["code"]}, message: ${info["message"]}"
                )
                return Result.failure(Exception("图片上传失败，请稍后重试"))
            }

            val uploadUrl = info["uploadUrl"] as? String
            android.util.Log.d("StorageDS", "uploadUrl: $uploadUrl")

            if (uploadUrl == null) {
                android.util.Log.e("StorageDS", "uploadUrl为null")
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
            android.util.Log.d("StorageDS", "COS上传响应码: ${response.code}")
            android.util.Log.d("StorageDS", "COS上传响应体: ${response.body?.string()}")

            if (response.isSuccessful || response.code == 204) {
                android.util.Log.d("StorageDS", "上传成功, downloadUrl: $downloadUrl")
                Result.success(downloadUrl)
            } else {
                android.util.Log.e("StorageDS", "上传失败: ${response.code}")
                Result.failure(Exception("图片上传失败，请稍后重试"))
            }
        } catch (e: Exception) {
            android.util.Log.e("StorageDS", "上传异常: ${e.message}", e)
            Result.failure(Exception("图片上传失败，请稍后重试"))
        }
    }
}