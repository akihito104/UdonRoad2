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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.request.RequestOptions
import com.freshdigitable.udonroad2.media.di.GlideApp
import com.freshdigitable.udonroad2.model.MediaEntity
import dagger.android.support.AndroidSupportInjection
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

class PhotoMediaFragment : Fragment() {

    companion object {
        private const val ARGS_URL = "url"
        fun create(mediaEntity: MediaEntity): PhotoMediaFragment {
            return PhotoMediaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_URL, mediaEntity.mediaUrl)
                }
            }
        }
    }

    private val mediaViewModel: MediaViewModel by viewModels(
        ownerProducer = { activity as AppCompatActivity }
    )

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
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.view_media_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnClickListener { mediaViewModel.toggleSystemUiVisibility.dispatch() }
        view.setOnTouchListener(object : View.OnTouchListener {
            private val LOWER_THRESHOLD = (90 - 10) * Math.PI / 180
            private val UPPER_THRESHOLD = (90 + 10) * Math.PI / 180
            private var oldEvent: MotionEvent? = null
                set(value) {
                    field?.recycle()
                    field = value
                }

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent?): Boolean {
                when (event?.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        oldEvent = MotionEvent.obtain(event)
                        return false
                    }
                    MotionEvent.ACTION_UP -> {
                        val old = oldEvent ?: return false
                        try {
                            val deltaX = event.getX(event.actionIndex) - old.getX(old.actionIndex)
                            val deltaY = event.getY(event.actionIndex) - old.getY(old.actionIndex)
                            val powDist = hypot(deltaX, deltaY)
                            if (powDist < ViewConfiguration.get(v.context).scaledTouchSlop) {
                                // maybe click
                                return false
                            }
                            val rad = atan2(deltaY, deltaX)
                            val absRad = abs(rad)
                            if (absRad > LOWER_THRESHOLD || absRad < UPPER_THRESHOLD) {
                                // maybe swipe to longitude
                                return true
                            }
                        } finally {
                            oldEvent = null
                        }
                    }
                }
                return false
            }
        })
        (view as? ScalableImageView)?.scaleListener = mediaViewModel

        val imageView = view as ImageView

        GlideApp.with(this)
            .load(url)
            .apply(RequestOptions().centerInside())
            .into(imageView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        view?.setOnClickListener(null)
    }
}
