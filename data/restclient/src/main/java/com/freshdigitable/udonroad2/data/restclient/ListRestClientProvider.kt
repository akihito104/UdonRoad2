package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.model.ListQuery
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KClass

class ListRestClientProvider @Inject constructor(
    private val providers: Map<Class<out ListQuery>, @JvmSuppressWildcards Provider<ListRestClient<out ListQuery, *>>>
) {
    fun <Q : ListQuery, T : ListRestClient<Q, *>> get(query: Q): T {
        val client = providers[query::class.java]?.get()
            ?: throw IllegalStateException(
                "ListRestClient: ${query::class.java.simpleName} is not registered list client."
            )

        @Suppress("UNCHECKED_CAST")
        return (client as T).also { it.query = query }
    }
}

@MustBeDocumented
@MapKey
@Retention
annotation class ListRestClientKey(val clazz: KClass<out ListQuery>)

@Module
interface TweetTimelineClientModule {
    @Binds
    @IntoMap
    @ListRestClientKey(ListQuery.Timeline::class)
    fun bindHomeTimelineClient(client: HomeTimelineClient): ListRestClient<out ListQuery, *>

    @Binds
    @IntoMap
    @ListRestClientKey(ListQuery.Fav::class)
    fun bindFavTimelineClient(client: FavTimelineClient): ListRestClient<out ListQuery, *>

    @Binds
    @IntoMap
    @ListRestClientKey(ListQuery.Media::class)
    fun bindMediaTimelineClient(client: MediaTimelineClient): ListRestClient<out ListQuery, *>
}

@Module
interface UserListClientModule {
    @Binds
    @IntoMap
    @ListRestClientKey(ListQuery.Follower::class)
    fun bindFollowerListClient(client: FollowerListClient): ListRestClient<out ListQuery, *>

    @Binds
    @IntoMap
    @ListRestClientKey(ListQuery.Following::class)
    fun bindFollowingListClient(client: FollowingListClient): ListRestClient<out ListQuery, *>
}

@Module
interface MemberListClientModule {
    @Binds
    @IntoMap
    @ListRestClientKey(ListQuery.UserListMembership::class)
    fun bindListMembershipListClient(client: ListMembershipListClient): ListRestClient<out ListQuery, *>
}