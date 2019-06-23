package com.freshdigitable.udonroad2.timeline

import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.UserListItem
import com.freshdigitable.udonroad2.timeline.listadapter.UserListAdapter
import kotlin.reflect.KClass

class UserListFragment : ListItemFragment<UserListViewModel, UserListItem>() {
    override val viewModelClass: KClass<UserListViewModel> = UserListViewModel::class

    override fun createListAdapter(viewModel: UserListViewModel): PagedListAdapter<UserListItem, *> {
        return UserListAdapter(viewModel)
    }
}
