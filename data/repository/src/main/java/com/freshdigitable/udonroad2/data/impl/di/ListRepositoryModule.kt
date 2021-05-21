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

package com.freshdigitable.udonroad2.data.impl.di

import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.LocalListDataSourceProvider
import com.freshdigitable.udonroad2.data.db.PagedListDataSourceFactoryProvider
import com.freshdigitable.udonroad2.data.impl.ListRepositoryImpl
import com.freshdigitable.udonroad2.data.impl.PagedListProviderImpl
import com.freshdigitable.udonroad2.data.local.di.LocalListDataSourceModule
import com.freshdigitable.udonroad2.data.local.di.PagedListDataSourceFactoryModule
import com.freshdigitable.udonroad2.data.restclient.RemoteListDataSourceProvider
import com.freshdigitable.udonroad2.data.restclient.di.CustomTimelineDataSourceModule
import com.freshdigitable.udonroad2.data.restclient.di.TweetTimelineDataSourceModule
import com.freshdigitable.udonroad2.data.restclient.di.UserListDataSourceModule
import com.freshdigitable.udonroad2.model.QueryType
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Module(
    includes = [
        LocalListDataSourceModule::class,
        TweetTimelineDataSourceModule::class,
        UserListDataSourceModule::class,
        CustomTimelineDataSourceModule::class,
        PagedListDataSourceFactoryModule::class
    ]
)
internal interface ListRepositoryModule

@Subcomponent(modules = [ListRepositoryModule::class])
interface ListRepositoryComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance query: QueryType): ListRepositoryComponent
    }

    val query: QueryType
    val localListDataSourceProvider: LocalListDataSourceProvider
    val remoteListDataSourceProvider: RemoteListDataSourceProvider
    val pagedListDataSourceFactoryProvider: PagedListDataSourceFactoryProvider
}

fun <Q : QueryType> ListRepositoryComponent.listRepository(): ListRepository<Q, Any> {
    return ListRepositoryImpl(
        localListDataSourceProvider.get(query as Q),
        remoteListDataSourceProvider.get(query as Q),
    )
}

fun <Q : QueryType, I : Any> ListRepositoryComponent.pagedListProvider(
    repository: ListRepository<Q, I>,
): PagedListProvider<Q, I> {
    return PagedListProviderImpl(
        pagedListDataSourceFactoryProvider.get(query as Q),
        repository,
    )
}

@Module(subcomponents = [ListRepositoryComponent::class])
interface ListRepositoryComponentModule
