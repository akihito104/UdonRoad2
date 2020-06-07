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
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.TweetingUser
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

class MemberListListViewModel(
    private val query: ListQuery.UserListMembership,
    private val owner: String,
    private val repository: ListRepository<ListQuery.UserListMembership>,
    private val navigator: NavigationDispatcher,
    pagedListProvider: PagedListProvider<ListQuery.UserListMembership, MemberListItem>
) : ListItemLoadable<ListQuery.UserListMembership, MemberListItem>, ViewModel() {
    override val loading: LiveData<Boolean>
        get() = repository.loading

    override fun onRefresh() {
        repository.loadList(query, owner)
    }

    override val timeline: LiveData<PagedList<MemberListItem>> =
        pagedListProvider.getList(query, owner)

    fun onUserIconClicked(user: TweetingUser) {
        navigator.postEvent(TimelineEvent.UserIconClicked(user))
    }

    fun onBodyItemClicked(memberList: MemberListItem) {
        navigator.postEvent(
            TimelineEvent.MemberListClicked(memberList)
        )
    }

    override fun onCleared() {
        super.onCleared()
        repository.clear(owner)
    }
}

@Module
interface MemberListListViewModelModule {
    companion object {
        @Provides
        fun provideMemberListListViewModel(
            query: ListQuery,
            owner: String,
            navigator: NavigationDispatcher,
            localListDataSourceProvider: LocalListDataSourceProvider,
            remoteListDataSourceProvider: RemoteListDataSourceProvider,
            pagedListDataSourceFactoryProvider: PagedListDataSourceFactoryProvider,
            executor: AppExecutor
        ): MemberListListViewModel {
            val q = query as ListQuery.UserListMembership
            val repository = ListRepository.create(
                q,
                localListDataSourceProvider,
                remoteListDataSourceProvider,
                executor
            )
            val pagedListProvider: PagedListProvider<ListQuery.UserListMembership, MemberListItem> =
                PagedListProvider.create(
                    pagedListDataSourceFactoryProvider.get(q),
                    repository,
                    executor
                )
            return MemberListListViewModel(query, owner, repository, navigator, pagedListProvider)
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(MemberListListViewModel::class)
    fun bindMemberListListViewModel(viewModel: MemberListListViewModel): ViewModel
}
