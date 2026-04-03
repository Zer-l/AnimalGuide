package com.permissionx.animalguide.data.remote.cloudbase

object AuthValidator {

    // 校验用户名（手机号转换后的username）
    fun validateUsername(username: String): Result<Unit> {
        if (username.isEmpty()) return Result.failure(Exception("用户名不能为空"))
        if (username.length > 64) return Result.failure(Exception("用户名不能超过64个字符"))
        val regex = Regex("^[a-zA-Z0-9][a-zA-Z0-9._-]*$")
        if (!regex.matches(username)) {
            return Result.failure(Exception("用户名只能包含字母、数字和._-，且只能以字母或数字开头"))
        }
        return Result.success(Unit)
    }

    // 校验密码
    fun validatePassword(password: String): Result<Unit> {
        if (password.length < 8) return Result.failure(Exception("密码至少需要8位字符"))
        if (password.length > 32) return Result.failure(Exception("密码不能超过32位字符"))

        // 不能以特殊字符开头
        val specialChars = "()!@#\$%^&*|?><_-"
        if (password.first().toString() in specialChars.map { it.toString() }) {
            return Result.failure(Exception("密码不能以特殊字符开头"))
        }

        // 至少包含三类字符
        var typeCount = 0
        if (password.any { it.isLowerCase() }) typeCount++
        if (password.any { it.isUpperCase() }) typeCount++
        if (password.any { it.isDigit() }) typeCount++
        if (password.any { it in "()!@#\$%^&*|?><_-" }) typeCount++

        if (typeCount < 3) {
            return Result.failure(Exception("密码需包含大小写字母、数字、特殊符号中的至少三种"))
        }

        return Result.success(Unit)
    }

    // 校验手机号
    fun validatePhone(phone: String): Result<Unit> {
        if (phone.length != 11) return Result.failure(Exception("请输入正确的手机号"))
        if (!phone.matches(Regex("^1[3-9]\\d{9}$"))) {
            return Result.failure(Exception("请输入正确的手机号"))
        }
        return Result.success(Unit)
    }

    // 生成username（user_ + 手机号）
    fun phoneToUsername(phone: String) = "user_$phone"
}