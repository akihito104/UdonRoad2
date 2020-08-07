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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.databinding.ActivityMainBinding
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
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
    lateinit var navigation: MainActivityNavigationDelegate

    @Inject
    lateinit var viewModelProvider: ViewModelProvider
    private val viewModel: MainViewModel by lazy { viewModelProvider[MainViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = findViewById<View>(R.id.main_drawer)?.let {
            DataBindingUtil.findBinding<ActivityMainBinding>(it)
        } ?: DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        viewModel.initialEvent(savedInstanceState?.savedViewState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
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

class MainViewModel(
    private val eventDispatcher: EventDispatcher,
    private val viewStates: MainActivityViewStates
) : ViewModel() {

    val isFabVisible: LiveData<Boolean> = viewStates.isFabVisible

    internal fun initialEvent(savedState: MainActivityViewState?) {
        eventDispatcher.postEvent(TimelineEvent.Setup(savedState))
    }

    fun onFabMenuSelected(item: MenuItem) {
        Timber.tag("MainViewModel").d("onFabSelected: $item")
        val selected =
            requireNotNull(viewStates.current?.selectedItem) { "selectedItem should not be null." }
        when (item.itemId) {
            com.freshdigitable.udonroad2.timeline.R.id.iffabMenu_main_detail -> {
                eventDispatcher.postEvent(
                    TimelineEvent.TweetDetailRequested(
                        selected.quoteId ?: requireNotNull(selected.originalId)
                    )
                )
            }
        }
    }

    fun onBackPressed() {
        eventDispatcher.postEvent(CommonEvent.Back(viewStates.current))
    }

    val currentState: MainActivityViewState?
        get() = viewStates.current
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
        @Provides
        fun provideMainViewModel(
            navigator: EventDispatcher,
            viewState: MainActivityViewStates
        ): MainViewModel {
            return MainViewModel(navigator, viewState)
        }
    }
}
