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
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetingUser
import com.freshdigitable.udonroad2.model.UserListItem
import com.freshdigitable.udonroad2.model.app.di.QueryTypeKey
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.ListOwner
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

class UserListViewModel(
    private val owner: ListOwner<QueryType.UserQueryType>,
    private val navigator: NavigationDispatcher,
    private val repository: ListRepository<QueryType.UserQueryType>,
    pagedListProvider: PagedListProvider<QueryType.UserQueryType, UserListItem>
) : ListItemLoadable<QueryType.UserQueryType, UserListItem>, ListItemClickListener<UserListItem>,
    ViewModel() {

    override val timeline: LiveData<PagedList<UserListItem>> =
        pagedListProvider.getList(owner.query, owner.value)

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

    override fun onCleared() {
        super.onCleared()
        repository.clear(owner.value)
    }

    override fun onBodyItemClicked(item: UserListItem) {
        Log.d("TimelineViewModel", "onBodyItemClicked: ${item.id}")
        navigator.postEvent(TimelineEvent.UserIconClicked(item))
    }

    override fun onUserIconClicked(user: TweetingUser) {
        navigator.postEvent(TimelineEvent.UserIconClicked(user))
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
            val o = owner as ListOwner<QueryType.UserQueryType>
            val repository = ListRepository.create(
                o.query,
                localListDataSourceProvider,
                remoteListDataSourceProvider,
                executor
            )
            val pagedListProvider: PagedListProvider<QueryType.UserQueryType, UserListItem> =
                PagedListProvider.create(
                    pagedListDataSourceFactoryProvider.get(o.query),
                    repository,
                    executor
                )
            return UserListViewModel(o, navigator, repository, pagedListProvider)
        }

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.UserQueryType::class)
        fun provideUserListViewModelKClass(): KClass<out ViewModel> = UserListViewModel::class
    }

    @Binds
    @IntoMap
    @ViewModelKey(UserListViewModel::class)
    fun bindUserListViewModel(viewModel: UserListViewModel): ViewModel
}
