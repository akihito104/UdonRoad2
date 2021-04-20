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
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.freshdigitable.udonroad2.media.databinding.ActivityMediaBinding
import com.freshdigitable.udonroad2.media.di.MediaViewModelComponent
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectDelegate
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.SnackbarFeedbackMessageDelegate
import com.freshdigitable.udonroad2.model.app.weakRef
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class MediaActivity : AppCompatActivity(), HasAndroidInjector {

    @Inject
    lateinit var viewModelComponentFactory: MediaViewModelComponent.Factory

    @Inject
    internal lateinit var activityEventDelegate: MediaActivityEffectDelegate
    private val viewModel: MediaViewModel by viewModels {
        viewModelComponentFactory.create(args.id, args.index).viewModelProviderFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityMediaBinding>(this, R.layout.activity_media)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        title = ""
        setSupportActionBar(binding.mediaToolbar)

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            viewModel.changeSystemUiVisibility.dispatch(visibility)
        }
        viewModel.systemUiVisibility.observe(this) {
            window.decorView.systemUiVisibility = it.visibility
            when (it) {
                SystemUiVisibility.SHOW -> supportActionBar?.show()
                SystemUiVisibility.HIDE -> supportActionBar?.hide()
                else -> throw IllegalStateException()
            }
        }
        lifecycleScope.launch {
            viewModel.navigationEvent.collect(activityEventDelegate::accept)
        }
        binding.mediaPager.setupPager(viewModel)
    }

    private fun ViewPager2.setupPager(viewModel: MediaViewModel) {
        val adapter = MediaAdapter(this@MediaActivity)
        this.adapter = adapter
        this.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.changeCurrentPosition.dispatch(position)
            }
        })

        viewModel.mediaItems.observe(this@MediaActivity) { items ->
            adapter.setItems(items)
        }
    }

    private val args: MediaActivityArgs by lazy {
        MediaActivityArgs.fromBundle(requireNotNull(intent.extras))
    }

    companion object {
        fun start(context: Context, args: MediaActivityArgs) {
            val intent = Intent(context, MediaActivity::class.java)
            intent.putExtras(args.toBundle())
            context.startActivity(intent)
        }
    }

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = injector
}

@BindingAdapter("currentPosition", "mediaSize", requireAll = false)
fun Toolbar.setCurrentPositionTitle(currentPosition: Int?, size: Int?) {
    title = when {
        currentPosition == null || size == null || size <= 0 -> ""
        else -> context.getString(R.string.media_current_position, currentPosition + 1, size)
    }
}

internal class MediaActivityEffectDelegate @Inject constructor(
    activity: MediaActivity,
) : ActivityEffectDelegate {
    private val feedbackDelegate = SnackbarFeedbackMessageDelegate(
        weakRef(activity) { it.findViewById(R.id.media_container) }
    )

    override fun accept(event: AppEffect) {
        feedbackDelegate.dispatchFeedbackMessage(event as FeedbackMessage)
    }
}
