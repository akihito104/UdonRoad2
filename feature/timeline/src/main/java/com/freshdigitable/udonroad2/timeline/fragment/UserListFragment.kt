package com.freshdigitable.udonroad2.timeline.fragment

import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.UserListItem
import com.freshdigitable.udonroad2.model.app.di.FragmentScope
import com.freshdigitable.udonroad2.timeline.listadapter.UserListAdapter
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModel
import dagger.Module
import dagger.android.ContributesAndroidInjector
import kotlin.reflect.KClass

class UserListFragment :
    ListItemFragment<UserListViewModel, QueryType.UserQueryType, UserListItem>() {
    override val viewModelClass: KClass<UserListViewModel> = UserListViewModel::class

    override fun createListAdapter(
        viewModel: UserListViewModel
    ): PagedListAdapter<UserListItem, *> {
        return UserListAdapter(viewModel)
    }
}

@Module
interface UserListFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributeUserListFragment(): UserListFragment
}
