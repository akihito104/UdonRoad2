package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.db.LocalListDataSourceModule
import com.freshdigitable.udonroad2.data.db.PagedListDataSourceFactoryModule
import com.freshdigitable.udonroad2.data.restclient.MemberListDataSourceModule
import com.freshdigitable.udonroad2.data.restclient.TweetTimelineDataSourceModule
import com.freshdigitable.udonroad2.data.restclient.UserListDataSourceModule
import dagger.Module

@Module(
    includes = [
        TweetRepositoryModule::class,
        UserRepositoryModule::class,
        RelationshipRepositoryModule::class,
        OAuthTokenRepositoryModule::class
    ]
)
interface RepositoryModule

@Module(
    includes = [
        LocalListDataSourceModule::class,
        TweetTimelineDataSourceModule::class,
        UserListDataSourceModule::class,
        MemberListDataSourceModule::class,
        PagedListDataSourceFactoryModule::class
    ]
)
interface ListRepositoryModule
