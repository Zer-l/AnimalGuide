package com.permissionx.animalguide.data.remote

import com.permissionx.animalguide.data.remote.dto.DoubaoRequest
import com.permissionx.animalguide.data.remote.dto.DoubaoResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DoubaoApi {

    @POST("api/v3/chat/completions")
    suspend fun generateAnimalInfo(
        @Header("Authorization") authorization: String,
        @Body request: DoubaoRequest
    ): DoubaoResponse
}