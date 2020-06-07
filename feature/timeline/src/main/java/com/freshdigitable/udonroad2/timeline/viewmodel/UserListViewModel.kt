package com.freshdigitable.udonroad2.timeline.viewmodel

import android.util.Log
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
import com.freshdigitable.udonroad2.model.UserListItem
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.ListOwner
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

class UserListViewModel(
    private val owner: ListOwner<ListQuery.UserListQuery>,
    private val navigator: NavigationDispatcher,
    private val repository: ListRepository<ListQuery.UserListQuery>,
    pagedListProvider: PagedListProvider<ListQuery.UserListQuery, UserListItem>
) : ListItemLoadable<ListQuery.UserListQuery, UserListItem>, ViewModel() {

    override val timeline: LiveData<PagedList<UserListItem>> =
        pagedListProvider.getList(owner.query, owner.value)

    override val loading: LiveData<Boolean>
        get() = repository.loading

    override fun onRefresh() {
        repository.loadList(owner.query, owner.value)
    }

    override fun onCleared() {
        super.onCleared()
        repository.clear(owner.value)
    }

    fun onBodyItemClicked(item: UserListItem) {
        Log.d("TimelineViewModel", "onBodyItemClicked: ${item.id}")
        navigator.postEvent(TimelineEvent.UserIconClicked(item))
    }

    fun onUserIconClicked(item: UserListItem) {
        navigator.postEvent(TimelineEvent.UserIconClicked(item))
    }
}

@Module
interface UserListViewModelModule {
    companion object {
        @Provides
        fun provideUserListViewModel(
            owner: ListOwner<*>,
            navigator: NavigationDispatcher,
            localListDataSourceProvider: LocalListDataSourceProvider,
            remoteListDataSourceProvider: RemoteListDataSourceProvider,
            pagedListDataSourceFactoryProvider: PagedListDataSourceFactoryProvider,
            executor: AppExecutor
        ): UserListViewModel {
            val o = owner as ListOwner<ListQuery.UserListQuery>
            val repository = ListRepository.create(
                o.query,
                localListDataSourceProvider,
                remoteListDataSourceProvider,
                executor
            )
            val pagedListProvider: PagedListProvider<ListQuery.UserListQuery, UserListItem> =
                PagedListProvider.create(
                    pagedListDataSourceFactoryProvider.get(o.query),
                    repository,
                    executor
                )
            return UserListViewModel(o, navigator, repository, pagedListProvider)
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(UserListViewModel::class)
    fun bindUserListViewModel(viewModel: UserListViewModel): ViewModel
}
