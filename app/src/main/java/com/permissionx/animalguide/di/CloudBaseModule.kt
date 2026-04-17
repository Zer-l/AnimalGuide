package com.permissionx.animalguide.di

import com.permissionx.animalguide.data.remote.cloudbase.CloudBaseClient
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.data.remote.cloudbase.AuthDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserDataSource
import com.permissionx.animalguide.data.remote.cloudbase.PostDataSource
import com.permissionx.animalguide.data.remote.cloudbase.CommentDataSource
import com.permissionx.animalguide.data.remote.cloudbase.LikeDataSource
import com.permissionx.animalguide.data.remote.cloudbase.FollowDataSource
import com.permissionx.animalguide.data.remote.cloudbase.CollectDataSource
import com.permissionx.animalguide.data.remote.cloudbase.StorageDataSource
import android.content.Context
import com.permissionx.animalguide.data.remote.cloudbase.DefaultImageHelper
import com.permissionx.animalguide.data.local.CachedPostDao
import com.permissionx.animalguide.data.local.CachedUserDao
import com.permissionx.animalguide.data.remote.cloudbase.SearchDataSource
import com.permissionx.animalguide.data.repository.AuthRepository
import com.permissionx.animalguide.data.repository.CommentRepository
import com.permissionx.animalguide.data.repository.FollowRepository
import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.data.repository.SearchRepository
import com.permissionx.animalguide.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudBaseModule {

    @Provides
    @Singleton
    fun provideCloudBaseClient(): CloudBaseClient = CloudBaseClient()

    @Provides
    @Singleton
    fun provideUserSessionManager(
        @ApplicationContext context: Context,
        cloudBaseClient: CloudBaseClient
    ): UserSessionManager {
        val manager = UserSessionManager(context, cloudBaseClient)
        // 设置回调，让 CloudBaseClient 能调用刷新逻辑
        cloudBaseClient.setSessionManagerProvider { manager }
        return manager
    }

    @Provides
    @Singleton
    fun provideAuthDataSource(
        client: CloudBaseClient
    ): AuthDataSource = AuthDataSource(client)

    @Provides
    @Singleton
    fun provideUserDataSource(
        client: CloudBaseClient
    ): UserDataSource = UserDataSource(client)

    @Provides
    @Singleton
    fun providePostDataSource(
        client: CloudBaseClient
    ): PostDataSource = PostDataSource(client)

    @Provides
    @Singleton
    fun provideCommentDataSource(
        client: CloudBaseClient
    ): CommentDataSource = CommentDataSource(client)

    @Provides
    @Singleton
    fun provideLikeDataSource(
        client: CloudBaseClient
    ): LikeDataSource = LikeDataSource(client)

    @Provides
    @Singleton
    fun provideStorageDataSource(
        client: CloudBaseClient,
        @ApplicationContext context: Context
    ): StorageDataSource = StorageDataSource(client, context)

    @Provides
    @Singleton
    fun providePostRepository(
        postDataSource: PostDataSource,
        likeDataSource: LikeDataSource,
        collectDataSource: CollectDataSource,
        storageDataSource: StorageDataSource,
        userSessionManager: UserSessionManager,
        userRepository: UserRepository,
        commentDataSource: CommentDataSource,
        cachedPostDao: CachedPostDao
    ): PostRepository = PostRepository(
        postDataSource, likeDataSource, collectDataSource, storageDataSource,
        userSessionManager, userRepository, commentDataSource, cachedPostDao
    )


    @Provides
    @Singleton
    fun provideAuthRepository(
        authDataSource: AuthDataSource,
        userDataSource: UserDataSource,
        postDataSource: PostDataSource,
        commentDataSource: CommentDataSource,
        likeDataSource: LikeDataSource,
        collectDataSource: CollectDataSource,
        followDataSource: FollowDataSource,
        userSessionManager: UserSessionManager,
        storageDataSource: StorageDataSource,
        defaultImageHelper: DefaultImageHelper,
        cachedUserDao: CachedUserDao
    ): AuthRepository = AuthRepository(
        authDataSource,
        userDataSource,
        postDataSource,
        commentDataSource,
        likeDataSource,
        collectDataSource,
        followDataSource,
        userSessionManager,
        storageDataSource,
        defaultImageHelper,
        cachedUserDao
    )

    @Provides
    @Singleton
    fun provideUserRepository(
        userDataSource: UserDataSource,
        storageDataSource: StorageDataSource,
        userSessionManager: UserSessionManager,
        cachedUserDao: CachedUserDao
    ): UserRepository =
        UserRepository(userDataSource, storageDataSource, userSessionManager, cachedUserDao)

    @Provides
    @Singleton
    fun provideCollectDataSource(client: CloudBaseClient): CollectDataSource =
        CollectDataSource(client)

    @Provides
    @Singleton
    fun provideCommentRepository(
        commentDataSource: CommentDataSource,
        likeDataSource: LikeDataSource,
        postDataSource: PostDataSource,
        userSessionManager: UserSessionManager,
        cachedCommentDao: com.permissionx.animalguide.data.local.CachedCommentDao
    ): CommentRepository = CommentRepository(
        commentDataSource, likeDataSource, postDataSource, userSessionManager, cachedCommentDao
    )

    @Provides
    @Singleton
    fun provideFollowDataSource(client: CloudBaseClient): FollowDataSource =
        FollowDataSource(client)

    @Provides
    @Singleton
    fun provideFollowRepository(
        followDataSource: FollowDataSource,
        userDataSource: UserDataSource,
        userSessionManager: UserSessionManager
    ): FollowRepository = FollowRepository(followDataSource, userDataSource, userSessionManager)

    @Provides
    @Singleton
    fun provideSearchDataSource(client: CloudBaseClient): SearchDataSource =
        SearchDataSource(client)

    @Provides
    @Singleton
    fun provideSearchRepository(
        searchDataSource: SearchDataSource,
        postRepository: PostRepository,
        userSessionManager: UserSessionManager,
        likeDataSource: LikeDataSource,
        collectDataSource: CollectDataSource
    ): SearchRepository = SearchRepository(
        searchDataSource, postRepository, userSessionManager, likeDataSource, collectDataSource
    )
}