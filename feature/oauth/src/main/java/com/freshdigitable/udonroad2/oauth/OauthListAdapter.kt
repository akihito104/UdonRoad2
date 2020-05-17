/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.oauth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.oauth.databinding.ViewOauthInputBinding
import com.freshdigitable.udonroad2.timeline.databinding.ViewTweetListItemBinding

internal class OauthListAdapter internal constructor(
    private val viewModel: OauthViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : PagedListAdapter<OauthItem, OauthViewHolder>(diffUtil) {

    override fun getItemId(position: Int): Long {
        return getItem(position)?.originalId ?: throw IllegalStateException()
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item?.originalId) {
            1L -> R.layout.view_oauth_input
            2L -> R.layout.view_tweet_list_item
            else -> throw IllegalStateException()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OauthViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.view_tweet_list_item -> OauthViewHolder.SampleTweetViewHolder(
                ViewTweetListItemBinding.inflate(inflater, parent, false)
            )
            R.layout.view_oauth_input -> OauthViewHolder.PinInputViewHolder(
                ViewOauthInputBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: OauthViewHolder, position: Int) {
        when (holder) {
            is OauthViewHolder.SampleTweetViewHolder -> {
                val item = getItem(position)
                holder.binding.tweet = item
            }
            is OauthViewHolder.PinInputViewHolder -> {
                holder.binding.viewModel = viewModel
                holder.binding.lifecycleOwner = viewLifecycleOwner
            }
        }
    }
}

internal sealed class OauthViewHolder(
    binding: ViewDataBinding
) : RecyclerView.ViewHolder(binding.root) {
    class SampleTweetViewHolder(val binding: ViewTweetListItemBinding) : OauthViewHolder(binding)
    class PinInputViewHolder(val binding: ViewOauthInputBinding) : OauthViewHolder(binding)
}

private val diffUtil = object : DiffUtil.ItemCallback<OauthItem>() {
    override fun areItemsTheSame(oldItem: OauthItem, newItem: OauthItem): Boolean =
        oldItem.originalId == newItem.originalId

    override fun areContentsTheSame(oldItem: OauthItem, newItem: OauthItem): Boolean =
        oldItem == newItem
}
