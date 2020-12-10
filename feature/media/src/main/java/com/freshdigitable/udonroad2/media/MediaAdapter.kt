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

package com.freshdigitable.udonroad2.media

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.MediaType

internal class MediaAdapter(
    activity: MediaActivity
) : FragmentStateAdapter(activity) {

    private val entities: MutableList<MediaEntity> = mutableListOf()

    fun setItems(entities: List<MediaEntity>) {
        this.entities.apply {
            clear()
            addAll(entities)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (entities[position].type) {
            MediaType.PHOTO -> R.layout.view_media_image
            MediaType.VIDEO, MediaType.ANIMATED_GIF -> R.layout.view_media_movie
        }
    }

    override fun createFragment(position: Int): Fragment {
        return when (getItemViewType(position)) {
            R.layout.view_media_image -> PhotoMediaFragment.create(entities[position])
            R.layout.view_media_movie -> MovieMediaFragment.create(entities[position])
            else -> throw IllegalStateException()
        }
    }

    override fun getItemCount(): Int = entities.size
}
