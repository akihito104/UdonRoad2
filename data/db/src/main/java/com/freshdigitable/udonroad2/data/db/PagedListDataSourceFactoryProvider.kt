/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data.db

import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.dao.MemberListListDao
import com.freshdigitable.udonroad2.data.db.dao.TweetListDao
import com.freshdigitable.udonroad2.data.db.dao.UserListDao
import com.freshdigitable.udonroad2.model.QueryType
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KClass

class PagedListDataSourceFactoryProvider @Inject constructor(
    private val providers: Map<Class<out QueryType>, @JvmSuppressWildcards Provider<PagedListProvider.DataSourceFactory<*>>>
) {
    fun <Q : QueryType, F : PagedListProvider.DataSourceFactory<*>> get(query: Q): F {
        val factory = providers[query::class.java]?.get()
            ?: providers.toList().firstOrNull { (clazz, _) ->
                clazz.isAssignableFrom(query::class.java)
            }?.second?.get()
            ?: throw IllegalStateException()

        @Suppress("UNCHECKED_CAST")
        return factory as F
    }
}

@MustBeDocumented
@MapKey
@Retention
annotation class PagedListDataSourceFactoryProviderKey(val clazz: KClass<out QueryType>)

@Module(includes = [DaoModule::class])
interface PagedListDataSourceFactoryModule {
    @Binds
    @IntoMap
    @PagedListDataSourceFactoryProviderKey(QueryType.TweetQueryType::class)
    fun bindTweetListDao(dao: TweetListDao): PagedListProvider.DataSourceFactory<*>

    @Binds
    @IntoMap
    @PagedListDataSourceFactoryProviderKey(QueryType.UserQueryType::class)
    fun bindUserListDao(dao: UserListDao): PagedListProvider.DataSourceFactory<*>

    @Binds
    @IntoMap
    @PagedListDataSourceFactoryProviderKey(QueryType.UserListMembership::class)
    fun bindMemberListListDao(dao: MemberListListDao): PagedListProvider.DataSourceFactory<*>
}
