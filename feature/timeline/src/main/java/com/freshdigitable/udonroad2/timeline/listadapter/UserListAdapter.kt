package com.freshdigitable.udonroad2.timeline.listadapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.model.user.UserListItem
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.UserIconClickListener
import com.freshdigitable.udonroad2.timeline.databinding.ViewUserListItemBinding

class UserListAdapter(
    private val clickListener: ListItemClickListener<UserListItem>,
    private val userIconClickListener: UserIconClickListener,
) : PagingDataAdapter<UserListItem, UserListViewHolder>(diffUtil) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserListViewHolder = UserListViewHolder(
        ViewUserListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
        val item = getItem(position) ?: return

        holder.binding.user = item
    }

    override fun onViewAttachedToWindow(holder: UserListViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.binding.viewModel = clickListener
        holder.binding.userIconClickListener = userIconClickListener
    }

    override fun onViewDetachedFromWindow(holder: UserListViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.binding.viewModel = null
        holder.binding.userIconClickListener = null
    }

    override fun getItemViewType(position: Int): Int = R.layout.view_tweet_list_item
}

private val diffUtil = object : DiffUtil.ItemCallback<UserListItem>() {
    override fun areItemsTheSame(
        oldItem: UserListItem,
        newItem: UserListItem
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: UserListItem,
        newItem: UserListItem
    ): Boolean = oldItem == newItem
}

class UserListViewHolder(
    internal val binding: ViewUserListItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    internal val root: ViewGroup = itemView as ViewGroup
}
