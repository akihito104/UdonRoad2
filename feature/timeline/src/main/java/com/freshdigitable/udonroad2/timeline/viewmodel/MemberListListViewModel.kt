package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
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
import com.freshdigitable.udonroad2.timeline.ListOwner
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

class MemberListListViewModel(
    private val repository: ListRepository<ListQuery.UserListMembership>,
    private val navigator: NavigationDispatcher,
    private val pagedListProvider: PagedListProvider<ListQuery.UserListMembership, MemberListItem>
) : ListItemLoadable<ListQuery.UserListMembership, MemberListItem>, ViewModel() {
    override val loading: LiveData<Boolean>
        get() = repository.loading

    override fun onRefresh() {
        val value = query.value ?: return
        repository.loadList(value.query, value.owner)
    }

    private val query = MutableLiveData<ListOwner<ListQuery.UserListMembership>?>()
    private val listItem: LiveData<PagedList<MemberListItem>> = query.switchMap { q ->
        when {
            q != null -> pagedListProvider.getList(q.query, q.owner)
            else -> MutableLiveData()
        }
    }

    override fun getList(listOwner: ListOwner<ListQuery.UserListMembership>): LiveData<PagedList<MemberListItem>> {
        query.value = listOwner
        return listItem
    }

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
        repository.clear(query.value?.owner ?: return)
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
                owner,
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
            return MemberListListViewModel(repository, navigator, pagedListProvider)
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(MemberListListViewModel::class)
    fun bindMemberListListViewModel(viewModel: MemberListListViewModel): ViewModel
}
