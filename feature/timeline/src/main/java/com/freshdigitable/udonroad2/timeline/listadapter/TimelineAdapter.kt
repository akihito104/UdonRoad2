package com.freshdigitable.udonroad2.timeline.listadapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.TweetListEventListener
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import com.freshdigitable.udonroad2.timeline.databinding.ViewTweetListItemBinding
import com.freshdigitable.udonroad2.timeline.databinding.ViewTweetListQuotedItemBinding

class TimelineAdapter(
    private val clickListener: TweetListItemClickListener,
    private val eventListener: TweetListEventListener,
    private val lifecycleOwner: LifecycleOwner
) : PagingDataAdapter<TweetListItem, TimelineAdapter.ViewHolder>(diffUtil) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder =
        ViewHolder(
            ViewTweetListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        holder.binding.tweet = item
        holder.binding.lifecycleOwner = lifecycleOwner
        if (item.quoted != null) {
            ensureQuotedView(holder).tweet = item
        } else {
            removeQuotedView(holder)
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.binding.apply {
            clickListener = this@TimelineAdapter.clickListener
            eventListener = this@TimelineAdapter.eventListener
        }
        holder.quotedView?.apply {
            clickListener = this@TimelineAdapter.clickListener
            eventListener = this@TimelineAdapter.eventListener
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.binding.apply {
            clickListener = null
            eventListener = null
        }
        holder.quotedView?.apply {
            clickListener = null
            eventListener = null
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        removeQuotedView(holder)
    }

    override fun getItemViewType(position: Int): Int =
        R.layout.view_tweet_list_item

    private fun ensureQuotedView(holder: ViewHolder): ViewTweetListQuotedItemBinding {
        return holder.quotedView
            ?: getQuotedViewBinding(holder.root).also {
                holder.root.addView(it.root, createQuotedItemLayoutParams(it.root.context))
                holder.quotedView = it
                it.lifecycleOwner = lifecycleOwner
            }
    }

    private val quotedViewCache: MutableList<ViewTweetListQuotedItemBinding> = mutableListOf()

    private fun removeQuotedView(holder: ViewHolder) {
        holder.quotedView?.let {
            holder.root.removeView(it.root)
            it.clickListener = null
            quotedViewCache.add(it)
        }
        holder.quotedView = null
    }

    private fun getQuotedViewBinding(constraintLayout: ViewGroup): ViewTweetListQuotedItemBinding {
        return if (quotedViewCache.isEmpty()) {
            ViewTweetListQuotedItemBinding.inflate(
                LayoutInflater.from(constraintLayout.context),
                constraintLayout, false
            )
        } else {
            quotedViewCache.removeAt(quotedViewCache.lastIndex)
        }
    }

    private fun createQuotedItemLayoutParams(context: Context): ViewGroup.LayoutParams {
        return ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = context.resources.getDimensionPixelSize(R.dimen.margin_half)
            topToBottom = R.id.tweetItem_via
            bottomToBottom =
                ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = R.id.tweetItem_names
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }
    }

    class ViewHolder(
        internal val binding: ViewTweetListItemBinding,
        internal var quotedView: ViewTweetListQuotedItemBinding? = null
    ) : RecyclerView.ViewHolder(binding.root) {
        internal val root: ViewGroup = itemView as ViewGroup
    }
}

private val diffUtil = object : DiffUtil.ItemCallback<TweetListItem>() {
    override fun areItemsTheSame(
        oldItem: TweetListItem,
        newItem: TweetListItem
    ): Boolean = oldItem.originalId == newItem.originalId

    override fun areContentsTheSame(
        oldItem: TweetListItem,
        newItem: TweetListItem
    ): Boolean = oldItem == newItem
}
