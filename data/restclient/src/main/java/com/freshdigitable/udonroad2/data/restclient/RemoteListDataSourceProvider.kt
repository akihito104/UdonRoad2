package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.QueryType.TweetQueryType
import com.freshdigitable.udonroad2.model.QueryType.UserQueryType
import com.freshdigitable.udonroad2.model.app.ClassKeyMap
import com.freshdigitable.udonroad2.model.app.valueByAssignableClassObject
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KClass

class RemoteListDataSourceProvider @Inject constructor(
    private val providers: ClassKeyMap<QueryType, Provider<RemoteListDataSource<out QueryType, *>>>
) {
    fun <Q : QueryType, T : RemoteListDataSource<Q, *>> get(query: Q): T {
        @Suppress("UNCHECKED_CAST")
        return providers.valueByAssignableClassObject(query).get() as T
    }
}

@MustBeDocumented
@MapKey
@Retention
annotation class RemoteListDataSourceKey(val clazz: KClass<out QueryType>)

@Module
interface TweetTimelineDataSourceModule {
    @Binds
    @IntoMap
    @RemoteListDataSourceKey(TweetQueryType.Timeline::class)
    fun bindHomeTimelineDataSource(
        dataSource: HomeTimelineDataSource
    ): RemoteListDataSource<out QueryType, *>

    @Binds
    @IntoMap
    @RemoteListDataSourceKey(TweetQueryType.Fav::class)
    fun bindFavTimelineDataSource(
        dataSource: FavTimelineDataSource
    ): RemoteListDataSource<out QueryType, *>

    @Binds
    @IntoMap
    @RemoteListDataSourceKey(TweetQueryType.Media::class)
    fun bindMediaTimelineDataSource(
        dataSource: MediaTimelineDataSource
    ): RemoteListDataSource<out QueryType, *>
}

@Module
interface UserListDataSourceModule {
    @Binds
    @IntoMap
    @RemoteListDataSourceKey(UserQueryType.Follower::class)
    fun bindFollowerListDataSource(
        dataSource: FollowerListDataSource
    ): RemoteListDataSource<out QueryType, *>

    @Binds
    @IntoMap
    @RemoteListDataSourceKey(UserQueryType.Following::class)
    fun bindFollowingListDataSource(
        dataSource: FollowingListDataSource
    ): RemoteListDataSource<out QueryType, *>
}

@Module
interface MemberListDataSourceModule {
    @Binds
    @IntoMap
    @RemoteListDataSourceKey(QueryType.UserListMembership::class)
    fun bindListMembershipListDataSource(
        dataSource: ListMembershipListDataSource
    ): RemoteListDataSource<out QueryType, *>
}
