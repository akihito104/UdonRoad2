/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.databinding.ActivityMainBinding
import com.freshdigitable.udonroad2.databinding.ViewTweetListItemBinding
import com.freshdigitable.udonroad2.databinding.ViewTweetListQuotedItemBinding
import com.freshdigitable.udonroad2.di.ViewModelKey
import com.freshdigitable.udonroad2.model.TweetListItem
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjection
import dagger.multibindings.IntoMap
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        val viewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        val listView = binding.mainList
        listView.layoutManager = LinearLayoutManager(this)
        val adapter = Adapter()
        listView.adapter = adapter
        viewModel.timeline.observe(this, Observer { list ->
            adapter.submitList(list)
        })
    }
}

private class Adapter : PagedListAdapter<TweetListItem, ViewHolder>(diffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ViewTweetListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { item ->
            holder.binding.tweet = item
            if (item.quoted != null) {
                ensureQuotedView(holder).tweet = item
            } else {
                removeQuotedView(holder)
            }
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
        ).also { lp ->
            lp.topMargin = context.resources.getDimensionPixelSize(R.dimen.margin_half)
            lp.topToBottom = R.id.tweetItem_via
            lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            lp.startToStart = R.id.tweetItem_names
            lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
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

@Module
interface MainActivityModule {
    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    fun bindMainViewModel(viewModel: MainViewModel) : ViewModel
}
