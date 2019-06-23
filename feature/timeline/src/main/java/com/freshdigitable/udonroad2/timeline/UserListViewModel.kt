package com.freshdigitable.udonroad2.timeline

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.repository.RepositoryComponent
import com.freshdigitable.udonroad2.data.repository.UserListRepository
import com.freshdigitable.udonroad2.model.UserListItem
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import dagger.Module
import dagger.Provides

class UserListViewModel(
    private val navigator: NavigationDispatcher,
    private val repository: UserListRepository
) : ListItemLoadable<UserListItem>, ViewModel() {

    private val listOwner = MutableLiveData<ListOwner>()

    val timeline: LiveData<PagedList<UserListItem>> = Transformations.switchMap(listOwner) {
        repository.getList("${it.id}", it.query)
    }

    override fun getList(listOwner: ListOwner): LiveData<PagedList<UserListItem>> {
        this.listOwner.postValue(listOwner)
        return timeline
    }

    override val loading: LiveData<Boolean>
        get() = repository.loading

    override fun onRefresh() {
        repository.loadAtFront()
    }

    override fun onCleared() {
        super.onCleared()
        repository.clear()
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
object UserListViewModelModule {
    @Provides
    @JvmStatic
    fun provideUserListViewModel(
        navigator: NavigationDispatcher,
        repositories: RepositoryComponent.Builder
    ): UserListViewModel {
        return UserListViewModel(navigator, repositories.build().userListRepository())
    }
}
