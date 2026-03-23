package com.permissionx.animalguide.di

import com.permissionx.animalguide.data.remote.BaiduApi
import com.permissionx.animalguide.data.remote.DoubaoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideBaiduApi(okHttpClient: OkHttpClient): BaiduApi {
        return Retrofit.Builder()
            .baseUrl("https://aip.baidubce.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BaiduApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDoubaoApi(okHttpClient: OkHttpClient): DoubaoApi {
        return Retrofit.Builder()
            .baseUrl("https://ark.cn-beijing.volces.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DoubaoApi::class.java)
    }
}