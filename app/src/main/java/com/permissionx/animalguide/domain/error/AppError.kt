package com.permissionx.animalguide.domain.error

sealed class AppError : Exception() {
    data class NetworkError(override val message: String = "网络连接失败，请检查网络后重试") : AppError()
    data class TimeoutError(override val message: String = "请求超时，请检查网络后重试") : AppError()
    data class ParseError(override val message: String = "未找到该动物的相关信息，请检查名称是否正确") : AppError()
    data class NotAnimalError(override val message: String = "未识别到动物，请换一张更清晰的图片重试") : AppError()
    data class NotFoundError(override val message: String = "生成科普内容失败，请重试") : AppError()
    data class UnknownError(override val message: String = "未知错误，请重试") : AppError()
}

fun Throwable.toAppError(): AppError = when (this) {
    is java.net.UnknownHostException -> AppError.NetworkError()
    is java.net.SocketTimeoutException -> AppError.TimeoutError()
    is AppError -> this
    else -> AppError.UnknownError(message ?: "未知错误")
}