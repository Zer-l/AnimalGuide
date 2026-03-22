package com.permissionx.animalguide.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BaiduTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: Long
)

data class BaiduAnimalResponse(
    @SerializedName("result") val result: List<AnimalResult>?,
    @SerializedName("error_msg") val errorMsg: String?
)

data class AnimalResult(
    @SerializedName("name") val name: String,
    @SerializedName("score") val score: String,
    @SerializedName("baike_info") val baikeInfo: BaikeInfo?
)

data class BaikeInfo(
    @SerializedName("description") val description: String?
)