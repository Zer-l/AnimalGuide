package com.permissionx.animalguide.ui.navigation

import android.net.Uri

object Routes {
    // 底部导航
    const val CAMERA = "camera"
    const val POKEDEX = "pokedex"
    const val HISTORY = "history"
    const val RESULT_NO_PARAM = "result"
    const val LOGIN = "login"
    const val SOCIAL = "social"
    const val SEARCH_SOCIAL = "search_social"
    const val PUBLISH = "publish"
    const val PUBLISH_WITH_ANIMAL = "publish/{animalName}"

    const val POKEDEX_DETAIL = "pokedex_detail/{animalName}"
    const val HISTORY_DETAIL = "history_detail/{historyId}"
    const val RESULT_FROM_HISTORY = "result_from_history/{imageUri}"

    const val POST_DETAIL = "post_detail/{postId}"
    const val ME = "me"
    const val SETTINGS = "settings"
    const val EDIT_PROFILE = "edit_profile"
    const val USER_PROFILE = "user_profile/{uid}"

    const val SET_PASSWORD = "set_password/{phone}/{verificationToken}"

    // 带参数的跳转方法
    fun pokedexDetail(animalName: String) = "pokedex_detail/${Uri.encode(animalName)}"
    fun historyDetail(historyId: Int) = "history_detail/$historyId"
    fun resultFromHistory(imageUri: String) = "result_from_history/${Uri.encode(imageUri)}"

    fun postDetail(postId: String) = "post_detail/$postId"
    fun userProfile(uid: String) = "user_profile/$uid"
    fun publishWithAnimal(animalName: String) = "publish/${Uri.encode(animalName)}"
    fun setPassword(phone: String, verificationToken: String) =
        "set_password/${Uri.encode(phone)}/${Uri.encode(verificationToken)}"
}