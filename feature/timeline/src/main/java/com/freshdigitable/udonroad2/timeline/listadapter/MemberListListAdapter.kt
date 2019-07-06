package com.freshdigitable.udonroad2.timeline.listadapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.timeline.MemberListListViewModel
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.databinding.ViewMemberListItemBinding

class MemberListListAdapter(
    private val viewModel: MemberListListViewModel
) : PagedListAdapter<MemberListItem, MemberListViewHolder>(diffUtil) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MemberListViewHolder =
        MemberListViewHolder(
            ViewMemberListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: MemberListViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.binding.memberList = item
    }

    override fun onViewAttachedToWindow(holder: MemberListViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.binding.viewModel = viewModel
    }

    override fun onViewDetachedFromWindow(holder: MemberListViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.binding.viewModel = null
    }

    override fun getItemViewType(position: Int): Int =
        R.layout.view_tweet_list_item

    override fun getItemId(position: Int): Long = getItem(position)?.id ?: -1
}

private val diffUtil = object : DiffUtil.ItemCallback<MemberListItem>() {
    override fun areItemsTheSame(
        oldItem: MemberListItem,
        newItem: MemberListItem
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: MemberListItem,
        newItem: MemberListItem
    ): Boolean = oldItem == newItem
}

class MemberListViewHolder(
    internal val binding: ViewMemberListItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    internal val root: ViewGroup = itemView as ViewGroup
}
