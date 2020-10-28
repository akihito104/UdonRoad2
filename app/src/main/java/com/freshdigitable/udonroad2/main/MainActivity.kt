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

package com.freshdigitable.udonroad2.main

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.databinding.ActivityMainBinding
import com.freshdigitable.udonroad2.input.TweetInputFragment
import com.freshdigitable.udonroad2.input.TweetInputFragmentArgs
import com.freshdigitable.udonroad2.input.TweetInputViewModel
import com.freshdigitable.udonroad2.input.di.TweetInputViewModelComponent
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    lateinit var navigation: MainActivityNavigationDelegate

    @Inject
    lateinit var viewModelProviderFactory: ViewModelProvider.Factory
    private val viewModel: MainViewModel by viewModels { viewModelProviderFactory }

    @Inject
    lateinit var tweetInputComponentFactory: TweetInputViewModelComponent.Factory
    private val tweetInputViewModel: TweetInputViewModel by viewModels {
        tweetInputComponentFactory.create(true)
            .viewModelProviderFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = findViewById<View>(R.id.main_drawer)?.let {
            DataBindingUtil.findBinding(it)
        } ?: DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setSupportActionBar(binding.mainToolbar)

        viewModel.initialEvent(savedInstanceState?.savedViewState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        supportFragmentManager.commit {
            add<TweetInputFragment>(
                binding.mainInputContainer.id,
                args = TweetInputFragmentArgs(true).toBundle()
            )
        }
        tweetInputViewModel.isExpanded.observe(this, object : Observer<Boolean> {
            private var navHostTitle: CharSequence? = null
            private var navIcon: Drawable? = null
            private var navIconDesc: CharSequence? = null
            override fun onChanged(expanded: Boolean?) {
                when (expanded) {
                    true -> {
                        if (navHostTitle != null) {
                            throw IllegalStateException("navHostTitle: $navHostTitle")
                        }
                        navHostTitle = supportActionBar?.title
                        supportActionBar?.setTitle(R.string.title_input_send_tweet)

                        navIconDesc = binding.mainToolbar.navigationContentDescription
                        binding.mainToolbar.setNavigationContentDescription(android.R.string.cancel)

                        navIcon = binding.mainToolbar.navigationIcon
                        binding.mainToolbar.setNavigationIcon(R.drawable.ic_clear_white)
                    }
                    false -> {
                        if (navHostTitle != null) {
                            supportActionBar?.title = navHostTitle
                            navHostTitle = null
                        }
                        if (navIcon != null) {
                            binding.mainToolbar.navigationIcon = navIcon
                            navIcon = null
                        }
                        if (navIconDesc != null) {
                            binding.mainToolbar.navigationContentDescription = navIconDesc
                            navIconDesc = null
                        }
                    }
                }
            }
        })

        binding.mainGlobalMenu.setNavigationItemSelectedListener { item ->
            val event = when (item.itemId) {
                R.id.drawer_menu_home -> TimelineEvent.Init
                R.id.drawer_menu_add_account -> OauthEvent.Init
                else -> null
            }
            if (event != null) {
//                navigation.navigator.postEvent(event)
                binding.mainDrawer.closeDrawer(binding.mainGlobalMenu)
            }
            return@setNavigationItemSelectedListener event != null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (tweetInputViewModel.isExpanded.value == true) {
            when (item.itemId) {
                android.R.id.home -> {
                    tweetInputViewModel.onCancelClicked()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.saveViewState(viewModel.currentState)
    }

    override fun onBackPressed() {
        if (tweetInputViewModel.isExpanded.value == true) {
            tweetInputViewModel.onCancelClicked()
            return
        }
        viewModel.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navigation.onSupportNavigateUp() || super.onSupportNavigateUp()
    }

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    companion object {
        private const val KEY_VIEW_STATE: String = "main_activity_view_state"

        private val Bundle?.savedViewState: MainActivityViewState?
            get() = this?.getSerializable(KEY_VIEW_STATE) as? MainActivityViewState

        private fun Bundle.saveViewState(viewState: MainActivityViewState?) {
            putSerializable(KEY_VIEW_STATE, viewState)
        }
    }
}
