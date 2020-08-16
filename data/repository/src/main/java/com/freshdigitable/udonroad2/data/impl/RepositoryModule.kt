package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.LocalListDataSourceModule
import com.freshdigitable.udonroad2.data.db.LocalListDataSourceProvider
import com.freshdigitable.udonroad2.data.db.PagedListDataSourceFactoryModule
import com.freshdigitable.udonroad2.data.db.PagedListDataSourceFactoryProvider
import com.freshdigitable.udonroad2.data.restclient.MemberListDataSourceModule
import com.freshdigitable.udonroad2.data.restclient.RemoteListDataSourceProvider
import com.freshdigitable.udonroad2.data.restclient.TweetTimelineDataSourceModule
import com.freshdigitable.udonroad2.data.restclient.UserListDataSourceModule
import com.freshdigitable.udonroad2.model.QueryType
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

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
    val appExecutor: AppExecutor
}

fun <Q : QueryType> ListRepositoryComponent.listRepository(): ListRepository<Q> {
    return ListRepositoryImpl<Q, Any>(
        localListDataSourceProvider.get(query as Q),
        remoteListDataSourceProvider.get(query as Q),
        appExecutor
    )
}

fun <Q : QueryType, I> ListRepositoryComponent.pagedListProvider(
    repository: ListRepository<Q>
): PagedListProvider<Q, I> {
    return PagedListProviderImpl(
        pagedListDataSourceFactoryProvider.get(query as Q),
        repository,
        appExecutor
    )
}

@Module(subcomponents = [ListRepositoryComponent::class])
interface ListRepositoryComponentModule
