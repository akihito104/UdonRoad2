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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.model.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjection
import dagger.multibindings.IntoMap
import kotlinx.android.synthetic.main.activity_media.media_pager
import javax.inject.Inject
import kotlin.math.min

class MediaActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)

        val layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        val adapter = MediaAdapter()
        media_pager.apply {
            this.layoutManager = layoutManager
            this.adapter = adapter
        }

        val viewModel = ViewModelProviders.of(this, viewModelFactory)[MediaViewModel::class.java]
        viewModel.tweet.observe(this) {
            val items = it?.body?.mediaItems ?: listOf()
            adapter.setItems(items)
            val pos = min(items.size - 1, index)
            media_pager.scrollToPosition(pos)
        }
        viewModel.setId(this.tweetId)
    }

    private val tweetId: Long
        get() = intent.getLongExtra(EXTRA_ID, -1)
    private val index: Int
        get() = intent.getIntExtra(EXTRA_INDEX, 0)

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_INDEX = "index"

        fun start(context: Context, id: Long, index: Int = 0) {
            val intent = Intent(context, MediaActivity::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_INDEX, index)
            }
            context.startActivity(intent)
        }
    }
}

@Module(includes = [MediaViewModelModule::class])
interface MediaActivityModule {
    @Binds
    @IntoMap
    @ViewModelKey(MediaViewModel::class)
    fun bindMediaViewModel(viewModel: MediaViewModel): ViewModel
}
