package com.freshdigitable.udonroad2.timeline

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.timeline.databinding.FragmentTimelineBinding
import com.freshdigitable.udonroad2.timeline.databinding.ViewTweetListItemBinding
import com.freshdigitable.udonroad2.timeline.databinding.ViewTweetListQuotedItemBinding
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class TimelineFragment : Fragment() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private lateinit var binding: FragmentTimelineBinding

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProviders.of(this, factory).get(TimelineViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val listView = binding.mainList
        val linearLayoutManager = LinearLayoutManager(view.context)
        listView.layoutManager = linearLayoutManager
        listView.addItemDecoration(DividerItemDecoration(view.context, linearLayoutManager.orientation))
        val adapter = Adapter()
        listView.adapter = adapter
        viewModel.timeline.observe(this, Observer { list ->
            adapter.submitList(list)
        })
    }
}

private class Adapter : PagedListAdapter<TweetListItem, ViewHolder>(diffUtil) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder =
        ViewHolder(ViewTweetListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        holder.binding.tweet = item
        if (item.quoted != null) {
            ensureQuotedView(holder).tweet = item
        } else {
            removeQuotedView(holder)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        removeQuotedView(holder)
    }

    override fun getItemViewType(position: Int): Int = R.layout.view_tweet_list_item

    override fun getItemId(position: Int): Long = getItem(position)?.originalId ?: -1

    override fun setHasStableIds(hasStableIds: Boolean) = super.setHasStableIds(true)

    private fun ensureQuotedView(holder: ViewHolder): ViewTweetListQuotedItemBinding {
        return holder.quotedView
            ?: getQuotedViewBinding(holder.root).also {
                holder.root.addView(it.root, createQuotedItemLayoutParams(it.root.context))
                holder.quotedView = it
            }
    }

    private val quotedViewCache: MutableList<ViewTweetListQuotedItemBinding> = mutableListOf()

    private fun removeQuotedView(holder: ViewHolder) {
        holder.quotedView?.let {
            holder.root.removeView(it.root)
            quotedViewCache.add(it)
        }
        holder.quotedView = null
    }

    private fun getQuotedViewBinding(constraintLayout: ViewGroup): ViewTweetListQuotedItemBinding {
        return if (quotedViewCache.isEmpty()) {
            ViewTweetListQuotedItemBinding.inflate(LayoutInflater.from(constraintLayout.context),
                constraintLayout, false)
        } else {
            quotedViewCache.removeAt(quotedViewCache.lastIndex)
        }
    }

    @NonNull
    private fun createQuotedItemLayoutParams(context: Context): ViewGroup.LayoutParams {
        return ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = context.resources.getDimensionPixelSize(R.dimen.margin_half)
            topToBottom = R.id.tweetItem_via
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = R.id.tweetItem_names
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }
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

private class ViewHolder(
    internal val binding: ViewTweetListItemBinding,
    internal var quotedView: ViewTweetListQuotedItemBinding? = null
) : RecyclerView.ViewHolder(binding.root) {
    internal val root: ViewGroup = itemView as ViewGroup
}