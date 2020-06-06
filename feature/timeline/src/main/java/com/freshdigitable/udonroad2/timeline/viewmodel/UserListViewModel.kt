package com.freshdigitable.udonroad2.timeline.viewmodel

import android.util.Log
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
    private val navigator: NavigationDispatcher,
    private val repository: ListRepository<ListQuery.UserListQuery>,
    private val pagedListProvider: PagedListProvider<ListQuery.UserListQuery, UserListItem>
) : ListItemLoadable<ListQuery.UserListQuery, UserListItem>, ViewModel() {

    private val listOwner = MutableLiveData<ListOwner<ListQuery.UserListQuery>>()

    val timeline: LiveData<PagedList<UserListItem>> = listOwner.switchMap {
        pagedListProvider.getList(it.query, it.owner)
    }

    override fun getList(listOwner: ListOwner<ListQuery.UserListQuery>): LiveData<PagedList<UserListItem>> {
        this.listOwner.postValue(listOwner)
        return timeline
    }

    override val loading: LiveData<Boolean>
        get() = repository.loading

    override fun onRefresh() {
        val owner = this.listOwner.value ?: return
        repository.loadList(owner.query, owner.owner)
    }

    override fun onCleared() {
        super.onCleared()
        repository.clear(listOwner.value?.owner ?: return)
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
            query: ListQuery,
            owner: String,
            navigator: NavigationDispatcher,
            localListDataSourceProvider: LocalListDataSourceProvider,
            remoteListDataSourceProvider: RemoteListDataSourceProvider,
            pagedListDataSourceFactoryProvider: PagedListDataSourceFactoryProvider,
            executor: AppExecutor
        ): UserListViewModel {
            val q = query as ListQuery.UserListQuery
            val repository = ListRepository.create(
                q,
                owner,
                localListDataSourceProvider,
                remoteListDataSourceProvider,
                executor
            )
            val pagedListProvider: PagedListProvider<ListQuery.UserListQuery, UserListItem> =
                PagedListProvider.create(
                    pagedListDataSourceFactoryProvider.get(q),
                    repository,
                    executor
                )
            return UserListViewModel(navigator, repository, pagedListProvider)
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(UserListViewModel::class)
    fun bindUserListViewModel(viewModel: UserListViewModel): ViewModel
}
