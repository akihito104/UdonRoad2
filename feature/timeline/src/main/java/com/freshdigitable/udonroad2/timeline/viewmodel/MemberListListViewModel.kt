package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.LocalListDataSourceProvider
import com.freshdigitable.udonroad2.data.db.PagedListDataSourceFactoryProvider
import com.freshdigitable.udonroad2.data.impl.AppExecutor
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.data.restclient.RemoteListDataSourceProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.di.QueryTypeKey
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

class MemberListListViewModel(
    private val owner: ListOwner<QueryType.UserListMembership>,
    private val repository: ListRepository<QueryType.UserListMembership>,
    private val eventDispatcher: EventDispatcher,
    pagedListProvider: PagedListProvider<QueryType.UserListMembership, MemberListItem>
) : ListItemLoadable<QueryType.UserListMembership, MemberListItem>,
    ListItemClickListener<MemberListItem>, ViewModel() {
    override val loading: LiveData<Boolean>
        get() = repository.loading

    override fun onRefresh() {
        val q = if (timeline.value?.isNotEmpty() == true) {
            ListQuery(owner.query, PageOption.OnHead())
        } else {
            ListQuery(owner.query, PageOption.OnInit)
        }
        repository.loadList(q, owner.value)
    }

    override val timeline: LiveData<PagedList<MemberListItem>> =
        pagedListProvider.getList(owner.query, owner.value)

    override fun onUserIconClicked(user: TweetingUser) {
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override fun onBodyItemClicked(item: MemberListItem) {
        eventDispatcher.postEvent(TimelineEvent.MemberListClicked(item))
    }

    override fun onCleared() {
        super.onCleared()
        repository.clear(owner.value)
    }
}

@Module
interface MemberListListViewModelModule {
    companion object {
        @Provides
        fun provideMemberListListViewModel(
            owner: ListOwner<*>,
            eventDispatcher: EventDispatcher,
            localListDataSourceProvider: LocalListDataSourceProvider,
            remoteListDataSourceProvider: RemoteListDataSourceProvider,
            pagedListDataSourceFactoryProvider: PagedListDataSourceFactoryProvider,
            executor: AppExecutor
        ): MemberListListViewModel {
            val o = owner as ListOwner<QueryType.UserListMembership>
            val repository = ListRepository.create(
                o.query,
                localListDataSourceProvider,
                remoteListDataSourceProvider,
                executor
            )
            val pagedListProvider: PagedListProvider<QueryType.UserListMembership, MemberListItem> =
                PagedListProvider.create(
                    pagedListDataSourceFactoryProvider.get(o.query),
                    repository,
                    executor
                )
            return MemberListListViewModel(o, repository, eventDispatcher, pagedListProvider)
        }

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.UserListMembership::class)
        fun provideMemberListListViewModelKClass(): KClass<out ViewModel> =
            MemberListListViewModel::class
    }

    @Binds
    @IntoMap
    @ViewModelKey(MemberListListViewModel::class)
    fun bindMemberListListViewModel(viewModel: MemberListListViewModel): ViewModel
}
