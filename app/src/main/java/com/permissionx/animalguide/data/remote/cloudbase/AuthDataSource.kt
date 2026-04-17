package com.permissionx.animalguide.data.remote.cloudbase

import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class AuthDataSource @Inject constructor(
    private val client: CloudBaseClient
) {
    private fun formatPhone(phoneNumber: String): String {
        val digits = phoneNumber.removePrefix("+86").removePrefix("86").trim()
        return "+86 $digits"
    }

    // 发送验证码
    suspend fun sendSmsCode(phoneNumber: String): Result<String> {
        val phone = if (phoneNumber.startsWith("+86")) phoneNumber else "+86 $phoneNumber"
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/auth/v1/verification",
            body = mapOf(
                "phone_number" to phone,
                "target" to "ANY"
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val verificationId = map["verification_id"] as? String
                if (verificationId != null) Result.success(verificationId)
                else Result.failure(Exception("发送验证码失败，请重试"))
            },
            onFailure = {
                Result.failure(Exception("发送验证码失败，请稍后重试"))
            }
        )
    }

    // 验证验证码，返回 verificationToken
    suspend fun verifySmsCode(
        verificationId: String,
        code: String
    ): Result<String> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/auth/v1/verification/verify",
            body = mapOf(
                "verification_id" to verificationId,
                "verification_code" to code
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val token = map["verification_token"] as? String
                if (token != null) Result.success(token)
                else Result.failure(Exception("验证码错误，请重试"))
            },
            onFailure = {
                // 统一替换为友好提示，不暴露底层错误
                Result.failure(Exception("验证码错误，请重试"))
            }
        )
    }

    // 登录或注册
    // loginOrRegister
    suspend fun loginOrRegister(
        phoneNumber: String,
        verificationToken: String,
        password: String? = null
    ): Result<Triple<String, String, String>> {
        val phone = formatPhone(phoneNumber)

        val loginResult = client.request<Map<String, Any>>(
            method = "POST",
            path = "/auth/v1/signin",
            body = mapOf(
                "phone_number" to phone,
                "verification_token" to verificationToken
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )

        if (loginResult.isSuccess) {
            val map = loginResult.getOrNull()!!
            val accessToken = map["access_token"] as? String
            val uid = map["sub"] as? String
            val refreshToken = map["refresh_token"] as? String ?: ""
            if (accessToken != null && uid != null) {
                return Result.success(Triple(accessToken, uid, refreshToken))
            }
        }

        val signUpBody = mutableMapOf<String, Any>(
            "phone_number" to phone,
            "verification_token" to verificationToken,
            "username" to "user_$phoneNumber"
        )
        password?.let { signUpBody["password"] = it }

        val signUpResult = client.request<Map<String, Any>>(
            method = "POST",
            path = "/auth/v1/signup",
            body = signUpBody,
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )

        return signUpResult.fold(
            onSuccess = { map ->
                val accessToken = map["access_token"] as? String
                val uid = map["sub"] as? String
                val refreshToken = map["refresh_token"] as? String ?: ""
                if (accessToken != null && uid != null) {
                    Result.success(Triple(accessToken, uid, refreshToken))
                } else {
                    Result.failure(Exception("注册失败，请重试"))
                }
            },
            onFailure = {
                Result.failure(Exception("登录失败，请稍后重试"))
            }
        )
    }

    // loginWithPassword
    suspend fun loginWithPassword(
        phoneNumber: String,
        password: String
    ): Result<Triple<String, String, String>> {
        val username = AuthValidator.phoneToUsername(phoneNumber)
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/auth/v1/signin",
            body = mapOf(
                "username" to username,
                "password" to password
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val accessToken = map["access_token"] as? String
                val uid = map["sub"] as? String
                val refreshToken = map["refresh_token"] as? String ?: ""
                if (accessToken != null && uid != null) {
                    Result.success(Triple(accessToken, uid, refreshToken))
                } else {
                    Result.failure(Exception("登录失败，请重试"))
                }
            },
            onFailure = {
                Result.failure(Exception("手机号或密码错误，请检查后重试"))
            }
        )
    }

    // 注销账号：删除 CloudBase Auth 账号，执行后 token 立即失效
    suspend fun deleteAuthAccount(): Result<Unit> {
        val result = client.request<Any>(
            method = "DELETE",
            path = "/auth/v1/me"
        )
        return result.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    // tryLogin
    suspend fun tryLogin(
        phoneNumber: String,
        verificationToken: String
    ): Result<Pair<String, String>> {
        val phone = formatPhone(phoneNumber)
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/auth/v1/signin",
            body = mapOf(
                "phone_number" to phone,
                "verification_token" to verificationToken
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val accessToken = map["access_token"] as? String
                val uid = map["sub"] as? String
                if (accessToken != null && uid != null) Result.success(Pair(accessToken, uid))
                else Result.failure(Exception("登录失败"))
            },
            onFailure = { Result.failure(Exception("登录失败")) }
        )
    }
}