package com.freshdigitable.udonroad2.timeline.listadapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.databinding.ViewCustomTimelineItemBinding
import com.freshdigitable.udonroad2.timeline.viewmodel.CustomTimelineListViewModel

internal class CustomTimelineListAdapter(
    private val viewModel: CustomTimelineListViewModel
) : PagingDataAdapter<CustomTimelineItem, CustomTimelineViewHolder>(diffUtil) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomTimelineViewHolder =
        CustomTimelineViewHolder(
            ViewCustomTimelineItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: CustomTimelineViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.binding.customTimeline = item
    }

    override fun onViewAttachedToWindow(holder: CustomTimelineViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.binding.viewModel = viewModel
    }

    override fun onViewDetachedFromWindow(holder: CustomTimelineViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.binding.viewModel = null
    }

    override fun getItemViewType(position: Int): Int =
        R.layout.view_tweet_list_item
}

private val diffUtil = object : DiffUtil.ItemCallback<CustomTimelineItem>() {
    override fun areItemsTheSame(
        oldItem: CustomTimelineItem,
        newItem: CustomTimelineItem
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: CustomTimelineItem,
        newItem: CustomTimelineItem
    ): Boolean = oldItem == newItem
}

class CustomTimelineViewHolder(
    internal val binding: ViewCustomTimelineItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    internal val root: ViewGroup = itemView as ViewGroup
}
