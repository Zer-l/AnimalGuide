package com.permissionx.animalguide.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DoubaoRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<DoubaoMessage>
)

data class DoubaoMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class DoubaoResponse(
    @SerializedName("choices") val choices: List<DoubaoChoice>?
)

data class DoubaoChoice(
    @SerializedName("message") val message: DoubaoMessage?
)