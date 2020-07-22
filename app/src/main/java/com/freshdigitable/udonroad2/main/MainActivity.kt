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

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.map
import androidx.lifecycle.observe
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.databinding.ActivityMainBinding
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentModule
import com.freshdigitable.udonroad2.timeline.fragment.TweetDetailFragmentModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.multibindings.IntoMap
import timber.log.Timber
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    lateinit var navigation: MainActivityNav

    @Inject
    lateinit var viewModelProvider: ViewModelProvider
    private var actionBarDrawerToggle: ActionBarDrawerToggle? = null
    val viewModel: MainViewModel by lazy { viewModelProvider[MainViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(
            this, R.layout.activity_main
        )

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        viewModel.initialEvent()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        viewModel.title.observe(this) { supportActionBar?.title = it }

        actionBarDrawerToggle = ActionBarDrawerToggle(this, binding.mainDrawer, 0, 0).apply {
            isDrawerIndicatorEnabled = true
            syncState()

            binding.mainDrawer.addDrawerListener(this)
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

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return actionBarDrawerToggle?.onOptionsItemSelected(item)
            ?: super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        actionBarDrawerToggle = null
    }

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}

class MainViewModel(
    private val navigator: NavigationDispatcher,
    private val viewSink: MainActivityViewSink
) : ViewModel() {

    private val state: LiveData<MainActivityViewState> = viewSink.state
    val isFabVisible: LiveData<Boolean> = state.map { it.fabVisible }
    val title: LiveData<String> = state.map { it.title }

    internal fun initialEvent() {
        navigator.postEvent(TimelineEvent.Setup)
    }

    fun onFabMenuSelected(item: MenuItem) {
        Timber.tag("MainViewModel").d("onFabSelected: $item")
        val selected =
            requireNotNull(viewSink.state.value?.selectedItem) { "selectedItem should not be null." }
        when (item.itemId) {
            com.freshdigitable.udonroad2.timeline.R.id.iffabMenu_main_detail -> {
                navigator.postEvent(
                    TimelineEvent.TweetDetailRequested(
                        selected.quoteId ?: requireNotNull(selected.originalId)
                    )
                )
            }
        }
    }

    fun onBackPressed() {
        navigator.postEvent(CommonEvent.Back(viewSink.state.value))
    }
}

@Module(
    includes = [
        ListItemFragmentModule::class,
        TweetDetailFragmentModule::class
    ]
)
interface MainActivityModule {
    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    fun bindMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    fun bindViewModelStoreOwner(activity: MainActivity): ViewModelStoreOwner

    companion object {
//        @Provides
//        fun provideNavigation(
//            navigator: NavigationDispatcher,
//            activity: MainActivity
//        ): Navigation<MainActivityState> {
//            return MainActivityNavigation(navigator, activity)
//        }

        @Provides
        fun provideMainViewModel(
            navigator: NavigationDispatcher,
            viewSink: MainActivityViewSink
        ): MainViewModel {
            return MainViewModel(navigator, viewSink)
        }
    }
}
