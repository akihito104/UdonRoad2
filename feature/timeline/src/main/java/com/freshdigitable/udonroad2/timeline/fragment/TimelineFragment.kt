package com.freshdigitable.udonroad2.timeline.fragment

import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.timeline.listadapter.TimelineAdapter
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModel
import dagger.Module
import dagger.android.ContributesAndroidInjector
import kotlin.reflect.KClass

class TimelineFragment :
    ListItemFragment<TimelineViewModel, QueryType.TweetQueryType, TweetListItem>() {
    override val viewModelClass: KClass<TimelineViewModel> = TimelineViewModel::class

    override fun createListAdapter(
        viewModel: TimelineViewModel
    ): PagedListAdapter<TweetListItem, *> = TimelineAdapter(viewModel, viewModel)
}

@Module
interface TimelineFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributeTimelineFragment(): TimelineFragment
}
