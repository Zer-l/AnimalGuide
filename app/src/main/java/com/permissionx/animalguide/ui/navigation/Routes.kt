package com.permissionx.animalguide.ui.navigation

import android.net.Uri

object Routes {
    // 底部导航
    const val CAMERA = "camera"
    const val POKEDEX = "pokedex"
    const val HISTORY = "history"
    const val RESULT_NO_PARAM = "result"

    const val POKEDEX_DETAIL = "pokedex_detail/{animalName}"
    const val HISTORY_DETAIL = "history_detail/{historyId}"
    const val RESULT_FROM_HISTORY = "result_from_history/{imageUri}"

    // 带参数的跳转方法
    fun pokedexDetail(animalName: String) = "pokedex_detail/${Uri.encode(animalName)}"
    fun historyDetail(historyId: Int) = "history_detail/$historyId"
    fun resultFromHistory(imageUri: String) = "result_from_history/${Uri.encode(imageUri)}"
}