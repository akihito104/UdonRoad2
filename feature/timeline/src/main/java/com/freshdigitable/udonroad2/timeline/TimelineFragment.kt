package com.freshdigitable.udonroad2.timeline

import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.timeline.listadapter.TimelineAdapter
import kotlin.reflect.KClass

class TimelineFragment : ListItemFragment<TimelineViewModel, TweetListItem>() {
    override val viewModelClass: KClass<TimelineViewModel> = TimelineViewModel::class

    override fun createListAdapter(
        viewModel: TimelineViewModel
    ): PagedListAdapter<TweetListItem, *> = TimelineAdapter(viewModel, viewModel)
}
