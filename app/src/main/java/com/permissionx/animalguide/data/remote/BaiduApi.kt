package com.permissionx.animalguide.data.remote

import com.permissionx.animalguide.data.remote.dto.BaiduAnimalResponse
import com.permissionx.animalguide.data.remote.dto.BaiduTokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BaiduApi {

    @GET("oauth/2.0/token")
    suspend fun getAccessToken(
        @Query("grant_type") grantType: String = "client_credentials",
        @Query("client_id") apiKey: String,
        @Query("client_secret") secretKey: String
    ): BaiduTokenResponse

    @FormUrlEncoded
    @POST("rest/2.0/image-classify/v1/animal")
    suspend fun recognizeAnimal(
        @Query("access_token") accessToken: String,
        @Field("image") imageBase64: String,
        @Field("top_num") topNum: Int,
        @Field("baike_num") baikeNum: Int
    ): BaiduAnimalResponse
}