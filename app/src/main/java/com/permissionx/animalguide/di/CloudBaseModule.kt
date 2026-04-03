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
import com.permissionx.animalguide.data.repository.AuthRepository
import com.permissionx.animalguide.data.repository.CommentRepository
import com.permissionx.animalguide.data.repository.FollowRepository
import com.permissionx.animalguide.data.repository.PostRepository
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
    ): UserSessionManager = UserSessionManager(context, cloudBaseClient)

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
        userRepository: UserRepository
    ): PostRepository = PostRepository(
        postDataSource, likeDataSource, collectDataSource, storageDataSource, userSessionManager, userRepository
    )


    @Provides
    @Singleton
    fun provideAuthRepository(
        authDataSource: AuthDataSource,
        userDataSource: UserDataSource,
        userSessionManager: UserSessionManager
    ): AuthRepository = AuthRepository(authDataSource, userDataSource, userSessionManager)

    @Provides
    @Singleton
    fun provideUserRepository(
        userDataSource: UserDataSource,
        storageDataSource: StorageDataSource,
        userSessionManager: UserSessionManager
    ): UserRepository = UserRepository(userDataSource, storageDataSource, userSessionManager)

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
        userSessionManager: UserSessionManager
    ): CommentRepository = CommentRepository(
        commentDataSource, likeDataSource, postDataSource, userSessionManager
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
}