/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data.repository

import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.ListDao
import com.freshdigitable.udonroad2.data.db.dao.MemberListListDao
import com.freshdigitable.udonroad2.data.db.dao.TweetListDao
import com.freshdigitable.udonroad2.data.db.dao.UserListDao
import com.freshdigitable.udonroad2.data.restclient.ListRestClient
import com.freshdigitable.udonroad2.data.restclient.ListRestClientProvider
import com.freshdigitable.udonroad2.data.restclient.MemberListClientModule
import com.freshdigitable.udonroad2.data.restclient.TweetTimelineClientModule
import com.freshdigitable.udonroad2.data.restclient.TwitterModule
import com.freshdigitable.udonroad2.data.restclient.UserListClientModule
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.MemberList
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.RepositoryScope
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.model.User
import com.freshdigitable.udonroad2.model.UserListItem
import dagger.Module
import dagger.Provides

@RepositoryScope
class TweetTimelineRepository(
    tweetDao: ListDao<TweetEntity, TweetListItem>,
    fetcher: ListFetcher<ListQuery, TweetEntity, ListRestClient<ListQuery, TweetEntity>, TweetListItem>,
    clientProvider: ListRestClientProvider,
    executor: AppExecutor
) : ListRepositoryImpl<TweetEntity, TweetListItem>(tweetDao, fetcher, clientProvider, executor)

@RepositoryScope
class UserListRepository(
    userDao: ListDao<User, UserListItem>,
    fetcher: ListFetcher<ListQuery, User, ListRestClient<ListQuery, User>, UserListItem>,
    clientProvider: ListRestClientProvider,
    executor: AppExecutor
) : ListRepositoryImpl<User, UserListItem>(userDao, fetcher, clientProvider, executor)

@RepositoryScope
class MemberListListRepository(
    memberListDao: ListDao<MemberList, MemberListItem>,
    fetcher: ListFetcher<ListQuery, MemberList, ListRestClient<ListQuery, MemberList>, MemberListItem>,
    clientProvider: ListRestClientProvider,
    executor: AppExecutor
) : ListRepositoryImpl<MemberList, MemberListItem>(memberListDao, fetcher, clientProvider, executor)

@Module(
    includes = [
        DaoModule::class,
        TwitterModule::class,
        TweetTimelineClientModule::class
    ]
)
object TimelineRepositoryModule {
    @Provides
    @JvmStatic
    @RepositoryScope
    fun provideTweetTimelineRepository(
        dao: TweetListDao,
        clientProvider: ListRestClientProvider,
        executor: AppExecutor
    ): TweetTimelineRepository {
        return TweetTimelineRepository(dao, TweetTimelineFetcher(), clientProvider, executor)
    }
}

@Module(
    includes = [
        DaoModule::class,
        TwitterModule::class,
        UserListClientModule::class
    ]
)
object UserListRepositoryModule {
    @Provides
    @JvmStatic
    @RepositoryScope
    fun provideUserListRepository(
        dao: UserListDao,
        clientProvider: ListRestClientProvider,
        executor: AppExecutor
    ): UserListRepository {
        return UserListRepository(dao, UserListFetcher(), clientProvider, executor)
    }
}

@Module(
    includes = [
        DaoModule::class,
        TwitterModule::class,
        MemberListClientModule::class
    ]
)
object MemberListListRepositoryModule {
    @Provides
    @JvmStatic
    @RepositoryScope
    fun provideMemberListListRepository(
        dao: MemberListListDao,
        clientProvider: ListRestClientProvider,
        executor: AppExecutor
    ): MemberListListRepository {
        return MemberListListRepository(dao, MemberListListFetcher(), clientProvider, executor)
    }
}
