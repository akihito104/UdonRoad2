package com.freshdigitable.udonroad2.timeline.fragment

import androidx.lifecycle.ViewModel
import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.UserListItem
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.timeline.listadapter.UserListAdapter
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModelModule
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

class UserListFragment : ListItemFragment<UserListViewModel, UserListItem>() {
    override val viewModelClass: KClass<UserListViewModel> = UserListViewModel::class

    override fun createListAdapter(viewModel: UserListViewModel): PagedListAdapter<UserListItem, *> {
        return UserListAdapter(viewModel)
    }
}

@Module(includes = [UserListViewModelModule::class])
interface UserListFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributeUserListFragment(): UserListFragment

    @Binds
    @IntoMap
    @ViewModelKey(UserListViewModel::class)
    fun bindUserListViewModel(viewModel: UserListViewModel): ViewModel
}
