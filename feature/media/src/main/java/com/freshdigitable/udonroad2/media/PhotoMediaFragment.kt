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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.freshdigitable.udonroad2.model.MediaItem
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class PhotoMediaFragment : Fragment() {

    companion object {
        private const val ARGS_URL = "url"
        fun create(mediaItem: MediaItem): PhotoMediaFragment {
            return PhotoMediaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_URL, mediaItem.mediaUrl)
                }
            }
        }
    }

    @Inject
    lateinit var viewModelProvider: ViewModelProvider.Factory

    private val url: String
        get() = requireNotNull(arguments?.getString(ARGS_URL)) {
            "url is required. use `${this::class.java.simpleName}.create()`."
        }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.view_media_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mediaViewModel =
            ViewModelProviders.of(requireActivity(), viewModelProvider)[MediaViewModel::class.java]
        view.setOnClickListener { mediaViewModel.toggleUiVisibility() }

        val imageView = view as ImageView

        Glide.with(this)
            .load(url)
            .apply(RequestOptions().centerInside())
            .into(imageView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        view?.setOnClickListener(null)
    }
}
