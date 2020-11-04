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

import android.animation.Animator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.databinding.ActivityMainBinding
import com.freshdigitable.udonroad2.input.TweetInputFragment
import com.freshdigitable.udonroad2.input.TweetInputFragmentArgs
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
        viewModel.appBarTitle.observe(this) {
            supportActionBar?.title = it?.invoke(this) ?: ""
        }

        supportFragmentManager.commit {
            add<TweetInputFragment>(
                binding.mainInputContainer.id,
                args = TweetInputFragmentArgs(true).toBundle()
            )
        }
        viewModel.isTweetInputMenuVisible.observe(this) {
            val tweetInputFragment =
                supportFragmentManager.findFragmentById(binding.mainInputContainer.id)
                    ?: return@observe
            tweetInputFragment.setMenuVisibility(it)
        }

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
        if (viewModel.isTweetInputExpanded) {
            when (item.itemId) {
                android.R.id.home -> {
                    viewModel.collapseTweetInput()
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

@BindingAdapter("appBarNavigationIcon")
fun Toolbar.bindNavigationIcon(type: NavigationIconType?) {
    if (type == null) {
        navigationIcon = null
        contentDescription = ""
        return
    }
    if (type == NavigationIconType.CLOSE) {
        setTag(R.id.tag_main_app_bar_nav_icon, navigationIcon)
        setNavigationIcon(R.drawable.ic_clear_white)
        // todo content description
        return
    }
    val description = when (type) {
        NavigationIconType.MENU -> R.string.nav_app_bar_open_drawer_description
        NavigationIconType.UP -> R.string.nav_app_bar_navigate_up_description
        else -> throw IllegalStateException()
    }
    contentDescription = context.getString(description)

    val icon = navigationIcon as? DrawerArrowDrawable
        ?: getTag(R.id.tag_main_app_bar_nav_icon) as? DrawerArrowDrawable
    if (icon == null) { // for the first time of set icon
        navigationIcon = DrawerArrowDrawable(context).apply {
            progress = 0f
        }
        return
    }
    navigationIcon = icon
    (getTag(R.id.tag_main_app_bar_nav_icon_anim) as? Animator)?.cancel()
    val start = icon.progress
    val end = when (type) {
        NavigationIconType.UP -> 1f
        else -> 0f
    }
    val anim = ObjectAnimator.ofFloat(icon, "progress", start, end)
    setTag(R.id.tag_main_app_bar_nav_icon_anim, anim)
    anim.start()
}
