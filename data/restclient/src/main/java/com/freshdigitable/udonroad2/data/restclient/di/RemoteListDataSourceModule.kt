/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad2.data.restclient.di

import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.data.restclient.CustomTimelineListDataSource
import com.freshdigitable.udonroad2.data.restclient.FollowerListDataSource
import com.freshdigitable.udonroad2.data.restclient.FollowingListDataSource
import com.freshdigitable.udonroad2.data.restclient.TimelineRemoteDataSource
import com.freshdigitable.udonroad2.model.QueryType
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@MustBeDocumented
@MapKey
@Retention
annotation class RemoteListDataSourceKey(val clazz: KClass<out QueryType>)

@Module
interface TweetTimelineDataSourceModule {
    @Binds
    @IntoMap
    @RemoteListDataSourceKey(QueryType.Tweet::class)
    fun bindHomeTimelineDataSource(
        dataSource: TimelineRemoteDataSource,
    ): RemoteListDataSource<out QueryType, *>
}

@Module
interface UserListDataSourceModule {
    @Binds
    @IntoMap
    @RemoteListDataSourceKey(QueryType.User.Follower::class)
    fun bindFollowerListDataSource(
        dataSource: FollowerListDataSource,
    ): RemoteListDataSource<out QueryType, *>

    @Binds
    @IntoMap
    @RemoteListDataSourceKey(QueryType.User.Following::class)
    fun bindFollowingListDataSource(
        dataSource: FollowingListDataSource,
    ): RemoteListDataSource<out QueryType, *>
}

@Module
interface CustomTimelineDataSourceModule {
    @Binds
    @IntoMap
    @RemoteListDataSourceKey(QueryType.CustomTimelineList::class)
    fun bindCustomTimelineListDataSource(
        dataSource: CustomTimelineListDataSource,
    ): RemoteListDataSource<out QueryType, *>
}
