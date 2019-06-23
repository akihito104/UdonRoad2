package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.repository.MemberListListRepository
import com.freshdigitable.udonroad2.data.repository.RepositoryComponent
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.TweetingUser
import dagger.Module
import dagger.Provides

class MemberListListViewModel(
    private val repository: MemberListListRepository
) : ListItemLoadable<MemberListItem>, ViewModel() {
    override val loading: LiveData<Boolean>
        get() = repository.loading

    override fun onRefresh() {
        repository.loadAtFront()
    }

    private val query =
        MutableLiveData<ListOwner?>()
    private val listItem = Transformations.switchMap(query) { q ->
        if (q != null) {
            repository.getList("${q.id}", q.query)
        } else {
            MutableLiveData()
        }
    }

    override fun getList(listOwner: ListOwner): LiveData<PagedList<MemberListItem>> {
        query.value = listOwner
        return listItem
    }

    fun onUserIconClicked(user: TweetingUser) {
    }
    fun onBodyItemClicked(memberList: MemberListItem) {
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
        repositoryComponent: RepositoryComponent.Builder
    ): MemberListListViewModel {
        return MemberListListViewModel(repositoryComponent.build().memberListListRepository())
    }
}
