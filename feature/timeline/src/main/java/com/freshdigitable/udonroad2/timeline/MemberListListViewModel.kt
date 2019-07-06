package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.repository.MemberListListRepository
import com.freshdigitable.udonroad2.data.repository.RepositoryComponent
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.TweetingUser
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import dagger.Module
import dagger.Provides

class MemberListListViewModel(
    private val repository: MemberListListRepository,
    private val navigator: NavigationDispatcher
) : ListItemLoadable<MemberListItem>, ViewModel() {
    override val loading: LiveData<Boolean>
        get() = repository.loading

    override fun onRefresh() {
        repository.loadAtFront()
    }

    private val query = MutableLiveData<ListOwner?>()
    private val listItem = query.switchMap { q ->
        when {
            q != null -> repository.getList("${q.id}", q.query)
            else -> MutableLiveData()
        }
    }

    override fun getList(listOwner: ListOwner): LiveData<PagedList<MemberListItem>> {
        query.value = listOwner
        return listItem
    }

    fun onUserIconClicked(user: TweetingUser) {
        navigator.postEvent(TimelineEvent.UserIconClicked(user))
    }

    fun onBodyItemClicked(memberList: MemberListItem) {
        navigator.postEvent(TimelineEvent.MemberListClicked(memberList))
    }

    override fun onCleared() {
        super.onCleared()
        repository.clear()
    }
}

@Module
object MemberListListViewModelModule {
    @JvmStatic
    @Provides
    fun provideMemberListListViewModel(
        repositoryComponent: RepositoryComponent.Builder,
        navigator: NavigationDispatcher
    ): MemberListListViewModel {
        return MemberListListViewModel(
            repositoryComponent.build().memberListListRepository(),
            navigator
        )
    }
}
