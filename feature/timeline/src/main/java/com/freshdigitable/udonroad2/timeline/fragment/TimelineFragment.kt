package com.freshdigitable.udonroad2.timeline.fragment

import androidx.lifecycle.ViewModel
import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.timeline.listadapter.TimelineAdapter
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModelModule
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

class TimelineFragment : ListItemFragment<TimelineViewModel, TweetListItem>() {
    override val viewModelClass: KClass<TimelineViewModel> = TimelineViewModel::class

    override fun createListAdapter(
        viewModel: TimelineViewModel
    ): PagedListAdapter<TweetListItem, *> = TimelineAdapter(viewModel, viewModel)
}

@Module(includes = [TimelineViewModelModule::class])
interface TimelineFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributeTimelineFragment(): TimelineFragment

    @Binds
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    fun bindTimelineViewModel(viewModel: TimelineViewModel): ViewModel
}
