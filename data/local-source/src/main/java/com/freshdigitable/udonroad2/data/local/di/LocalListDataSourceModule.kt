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

package com.freshdigitable.udonroad2.data.local.di

import com.freshdigitable.udonroad2.data.LocalListDataSource
import com.freshdigitable.udonroad2.data.db.dao.ConversationListDao
import com.freshdigitable.udonroad2.data.db.dao.CustomTimelineListDao
import com.freshdigitable.udonroad2.data.db.dao.TweetListDao
import com.freshdigitable.udonroad2.data.db.dao.UserListDao
import com.freshdigitable.udonroad2.model.QueryType
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@MustBeDocumented
@MapKey
@Retention
annotation class LocalListDataSourceKey(val clazz: KClass<out QueryType>)

@Module(includes = [DaoModule::class])
interface LocalListDataSourceModule {
    @Binds
    @IntoMap
    @LocalListDataSourceKey(QueryType.TweetQueryType::class)
    fun bindTweetListDao(dao: TweetListDao): LocalListDataSource<out QueryType, *>

    @Binds
    @IntoMap
    @LocalListDataSourceKey(QueryType.TweetQueryType.Conversation::class)
    fun bindConversationListDao(dao: ConversationListDao): LocalListDataSource<out QueryType, *>

    @Binds
    @IntoMap
    @LocalListDataSourceKey(QueryType.UserQueryType::class)
    fun bindUserListDao(dao: UserListDao): LocalListDataSource<out QueryType, *>

    @Binds
    @IntoMap
    @LocalListDataSourceKey(QueryType.CustomTimelineListQueryType.Membership::class)
    fun bindCustomTimelineListDao(dao: CustomTimelineListDao): LocalListDataSource<out QueryType, *>
}
