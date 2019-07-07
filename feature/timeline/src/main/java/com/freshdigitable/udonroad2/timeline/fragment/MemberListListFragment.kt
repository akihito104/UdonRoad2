package com.freshdigitable.udonroad2.timeline.fragment

import androidx.lifecycle.ViewModel
import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.timeline.listadapter.MemberListListAdapter
import com.freshdigitable.udonroad2.timeline.viewmodel.MemberListListViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.MemberListListViewModelModule
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

class MemberListListFragment : ListItemFragment<MemberListListViewModel, MemberListItem>() {
    override val viewModelClass: KClass<MemberListListViewModel> = MemberListListViewModel::class

    override fun createListAdapter(viewModel: MemberListListViewModel): PagedListAdapter<MemberListItem, *> {
        return MemberListListAdapter(viewModel)
    }
}

@Module(includes = [MemberListListViewModelModule::class])
interface MemberListListFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributeMemberListListFragment(): MemberListListFragment

    @Binds
    @IntoMap
    @ViewModelKey(MemberListListViewModel::class)
    fun bindMemberListListViewModel(viewModel: MemberListListViewModel): ViewModel
}
