package com.permissionx.animalguide.di

import android.content.Context
import com.permissionx.animalguide.data.local.AnimalDao
import com.permissionx.animalguide.data.local.AnimalPhotoDao
import com.permissionx.animalguide.data.local.HistoryDao
import com.permissionx.animalguide.data.remote.BaiduApi
import com.permissionx.animalguide.data.remote.DoubaoApi
import com.permissionx.animalguide.data.repository.HistoryRepository
import com.permissionx.animalguide.data.repository.PhotoRepository
import com.permissionx.animalguide.data.repository.RecognizeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideDoubaoApi(@DoubaoClient okHttpClient: OkHttpClient): DoubaoApi {
        return Retrofit.Builder()
            .baseUrl("https://ark.cn-beijing.volces.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DoubaoApi::class.java)
    }

    @Provides
    @Singleton
    @DoubaoClient
    fun provideDoubaoOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRecognizeRepository(
        baiduApi: BaiduApi,
        doubaoApi: DoubaoApi,
        @ApplicationContext context: Context
    ): RecognizeRepository {
        return RecognizeRepository(baiduApi, doubaoApi, context)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(
        historyDao: HistoryDao,
        animalDao: AnimalDao,
        animalPhotoDao: AnimalPhotoDao
    ): HistoryRepository {
        return HistoryRepository(historyDao, animalDao, animalPhotoDao)
    }

    @Provides
    @Singleton
    fun providePhotoRepository(
        animalPhotoDao: AnimalPhotoDao,
        animalDao: AnimalDao,
        historyDao: HistoryDao
    ): PhotoRepository {
        return PhotoRepository(animalPhotoDao, animalDao, historyDao)
    }
}