package com.freshdigitable.udonroad2.timeline.fragment

import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.QueryType.UserListMembership
import com.freshdigitable.udonroad2.timeline.listadapter.MemberListListAdapter
import com.freshdigitable.udonroad2.timeline.viewmodel.MemberListListViewModel
import dagger.Module
import dagger.android.ContributesAndroidInjector
import kotlin.reflect.KClass

class MemberListListFragment :
    ListItemFragment<MemberListListViewModel, UserListMembership, MemberListItem>() {
    override val viewModelClass: KClass<MemberListListViewModel> = MemberListListViewModel::class

    override fun createListAdapter(
        viewModel: MemberListListViewModel
    ): PagedListAdapter<MemberListItem, *> {
        return MemberListListAdapter(viewModel)
    }
}

@Module
interface MemberListListFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributeMemberListListFragment(): MemberListListFragment
}
