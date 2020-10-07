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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.viewpager2.widget.ViewPager2
import com.freshdigitable.udonroad2.media.databinding.ActivityMediaBinding
import com.freshdigitable.udonroad2.model.app.di.FragmentScope
import com.freshdigitable.udonroad2.model.tweet.TweetId
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class MediaActivity : AppCompatActivity(), HasAndroidInjector {

    @Inject
    lateinit var viewModelProviderFactory: ViewModelProvider.Factory
    private val viewModel: MediaViewModel by viewModels { viewModelProviderFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityMediaBinding>(this, R.layout.activity_media)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        title = ""
        setSupportActionBar(binding.mediaToolbar)
        viewModel.isInImmersive.observe(this) {
            if (it) {
                supportActionBar?.hide()
            } else {
                supportActionBar?.show()
            }
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            viewModel.onSystemUiVisibilityChange(visibility)
        }
        viewModel.systemUiVisibility.observe(this) {
            window.decorView.systemUiVisibility = it.visibility
        }

        binding.mediaPager.setupPager(viewModel)
        viewModel.setTweetId(this.tweetId)
    }

    private fun ViewPager2.setupPager(viewModel: MediaViewModel) {
        val adapter = MediaAdapter(this@MediaActivity)
        this.adapter = adapter
        this.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.setCurrentPosition(position)
            }
        })

        viewModel.mediaItems.observe(this@MediaActivity) { items ->
            adapter.setItems(items)
        }
        viewModel.currentPosition.observe(this@MediaActivity) {
            if (it != null) {
                this.setCurrentItem(it, false)
            }
        }
        viewModel.setCurrentPosition(index)
    }

    private val args: MediaActivityArgs by lazy {
        MediaActivityArgs.fromBundle(requireNotNull(intent.extras))
    }
    private val tweetId: TweetId get() = args.id
    private val index: Int get() = args.index

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
        currentPosition == null || size == null || size > 0 -> ""
        else -> context.getString(R.string.media_current_position, currentPosition + 1, size)
    }
}

@Module(includes = [MediaViewModelModule::class])
interface MediaActivityModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributePhotoMediaFragment(): PhotoMediaFragment

    @FragmentScope
    @ContributesAndroidInjector
    fun contributeMovieMediaFragment(): MovieMediaFragment
}
