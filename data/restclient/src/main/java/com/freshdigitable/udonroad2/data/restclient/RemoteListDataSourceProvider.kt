package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.ListQuery.TweetListQuery
import com.freshdigitable.udonroad2.model.ListQuery.UserListQuery
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KClass

class RemoteListDataSourceProvider @Inject constructor(
    private val providers: Map<Class<out ListQuery>, @JvmSuppressWildcards Provider<RemoteListDataSource<out ListQuery, *>>>
) {
    fun <Q : ListQuery, T : RemoteListDataSource<Q, *>> get(query: Q): T {
        val dataSource = providers[query::class.java]?.get()
            ?: throw IllegalStateException(
                "ListRestClient: ${query::class.java.simpleName} is not registered list client."
            )

        @Suppress("UNCHECKED_CAST")
        return dataSource as T
    }
}

@MustBeDocumented
@MapKey
@Retention
annotation class RemoteListDataSourceKey(val clazz: KClass<out ListQuery>)

@Module
interface TweetTimelineDataSourceModule {
    @Binds
    @IntoMap
    @RemoteListDataSourceKey(TweetListQuery.Timeline::class)
    fun bindHomeTimelineDataSource(
        dataSource: HomeTimelineDataSource
    ): RemoteListDataSource<out ListQuery, *>

    @Binds
    @IntoMap
    @RemoteListDataSourceKey(TweetListQuery.Fav::class)
    fun bindFavTimelineDataSource(
        dataSource: FavTimelineDataSource
    ): RemoteListDataSource<out ListQuery, *>

    @Binds
    @IntoMap
    @RemoteListDataSourceKey(TweetListQuery.Media::class)
    fun bindMediaTimelineDataSource(
        dataSource: MediaTimelineDataSource
    ): RemoteListDataSource<out ListQuery, *>
}

@Module
interface UserListDataSourceModule {
    @Binds
    @IntoMap
    @RemoteListDataSourceKey(UserListQuery.Follower::class)
    fun bindFollowerListDataSource(
        dataSource: FollowerListDataSource
    ): RemoteListDataSource<out ListQuery, *>

    @Binds
    @IntoMap
    @RemoteListDataSourceKey(UserListQuery.Following::class)
    fun bindFollowingListDataSource(
        dataSource: FollowingListDataSource
    ): RemoteListDataSource<out ListQuery, *>
}

@Module
interface MemberListDataSourceModule {
    @Binds
    @IntoMap
    @RemoteListDataSourceKey(ListQuery.UserListMembership::class)
    fun bindListMembershipListDataSource(
        dataSource: ListMembershipListDataSource
    ): RemoteListDataSource<out ListQuery, *>
}
