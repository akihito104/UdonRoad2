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
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.databinding.ActivityMainBinding
import com.freshdigitable.udonroad2.databinding.NavHeaderBinding
import com.freshdigitable.udonroad2.input.TweetInputFragment
import com.freshdigitable.udonroad2.input.TweetInputFragmentArgs
import com.google.android.material.navigation.NavigationView
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    internal lateinit var navigation: MainActivityNavigationDelegate

    @Inject
    lateinit var viewModelProviderFactory: ViewModelProvider.Factory
    private val viewModel: MainViewModel by viewModels { viewModelProviderFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = findViewById<View>(R.id.main_drawer)?.let {
            DataBindingUtil.findBinding<ActivityMainBinding>(it)
        } ?: DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setSupportActionBar(binding.mainToolbar)

        lifecycle.addObserver(navigation)
        lifecycleScope.launch {
            viewModel.effect.collect { navigation.accept(it) }
        }

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        viewModel.appBarTitle.observe(this) {
            supportActionBar?.title = it?.invoke(this) ?: ""
        }

        setupTweetInput()
        viewModel.isTweetInputMenuVisible.observe(this) {
            val tweetInputFragment =
                supportFragmentManager.findFragmentById(binding.mainInputContainer.id)
                    ?: return@observe
            tweetInputFragment.setMenuVisibility(it)
        }

        binding.mainDrawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                viewModel.showDrawerMenu.dispatch()
            }

            override fun onDrawerClosed(drawerView: View) {
                viewModel.hideDrawerMenu.dispatch()
            }
        })
        binding.mainGlobalMenu.setNavigationItemSelectedListener { item ->
            if (item.groupId != R.id.menu_group_drawer_switchable_accounts) {
                val handled = navigation.onNavDestinationSelected(item)
                if (handled) {
                    binding.mainDrawer.closeDrawer(binding.mainGlobalMenu)
                    return@setNavigationItemSelectedListener true
                }
            }
            viewModel.onDrawerMenuItemClicked(item.groupId, item.itemId, item.title)
        }
        val headerView = binding.mainGlobalMenu.getHeaderView(0)
        val navHeaderBinding: NavHeaderBinding = requireNotNull<NavHeaderBinding>(
            DataBindingUtil.findBinding(headerView) ?: DataBindingUtil.bind(headerView)
        )
        navHeaderBinding.also {
            it.viewModel = viewModel
            it.lifecycleOwner = this
        }
        viewModel.drawerState.observe(this) {
            binding.mainDrawer.bindOpenState(it.isOpened, binding.mainGlobalMenu)
            binding.mainGlobalMenu.bindMenuState(it)
        }

        viewModel.initialEvent()
    }

    private fun setupTweetInput() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main_inputContainer)
        if (fragment != null) {
            return
        }
        supportFragmentManager.commit {
            add<TweetInputFragment>(
                R.id.main_inputContainer,
                args = TweetInputFragmentArgs(true).toBundle()
            )
        }
    }

    private fun DrawerLayout.bindOpenState(isOpened: Boolean, navView: NavigationView) {
        if (isOpened && !isDrawerOpen(navView)) {
            openDrawer(navView)
        } else if (!isOpened && isDrawerOpen(navView)) {
            closeDrawer(navView)
        }
    }

    private fun NavigationView.bindMenuState(state: DrawerViewModel.State) {
        menu.apply {
            removeGroup(R.id.menu_group_drawer_switchable_accounts)
            state.switchableAccounts.map { it.account }
                .forEachIndexed { i, screenName ->
                    add(R.id.menu_group_drawer_switchable_accounts, Menu.NONE, i, screenName)
                }

            val accountSwitcherOpened = state.isAccountSwitcherOpened
            setGroupVisible(R.id.menu_group_drawer_switchable_accounts, accountSwitcherOpened)
            setGroupVisible(R.id.menu_group_drawer_register_account, accountSwitcherOpened)
            setGroupVisible(R.id.menu_group_drawer_default, !accountSwitcherOpened)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (viewModel.currentState.isTweetInputExpanded) {
            when (item.itemId) {
                android.R.id.home -> {
                    viewModel.collapseTweetInput()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (viewModel.onBackPressed()) return
        // https://issuetracker.google.com/issues/139738913
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            val shouldFinishForBackStackCount = supportFragmentManager.run {
                (primaryNavigationFragment?.childFragmentManager?.backStackEntryCount ?: 0) == 0 &&
                    backStackEntryCount == 0
            }
            if (isTaskRoot && shouldFinishForBackStackCount) {
                finishAfterTransition()
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navigation.onSupportNavigateUp() || super.onSupportNavigateUp()
    }

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
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
