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
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.databinding.ActivityMainBinding
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.Navigation
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.oauth.OauthFragmentModule
import com.freshdigitable.udonroad2.timeline.SelectedItemId
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.fragment.MemberListListFragmentModule
import com.freshdigitable.udonroad2.timeline.fragment.TimelineFragmentModule
import com.freshdigitable.udonroad2.timeline.fragment.TweetDetailFragmentModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.multibindings.IntoMap
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    lateinit var navigation: Navigation<MainActivityState>

    @Inject
    lateinit var viewModelProvider: ViewModelProvider
    private var actionBarDrawerToggle: ActionBarDrawerToggle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(
            this, R.layout.activity_main
        )

        val viewModel = viewModelProvider[MainViewModel::class.java]
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        viewModel.initialEvent()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

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
                navigation.navigator.postEvent(event)
                binding.mainDrawer.closeDrawer(binding.mainGlobalMenu)
            }
            return@setNavigationItemSelectedListener event != null
        }
    }

    override fun onBackPressed() {
        navigation.navigator.postEvent(TimelineEvent.Back)
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
    private val oauthTokenRepository: OAuthTokenRepository
) : ViewModel() {

    private val selectedItemId = MutableLiveData<SelectedItemId?>()
    private val fabVisible = MutableLiveData<Boolean>()
    private val _isFabVisible = MediatorLiveData<Boolean>()
    val isFabVisible: LiveData<Boolean> = _isFabVisible

    init {
        _isFabVisible.addSource(selectedItemId) { updateFabVisible() }
        _isFabVisible.addSource(fabVisible) { updateFabVisible() }
    }

    internal fun initialEvent() {
        val event = when {
            oauthTokenRepository.getCurrentUserId() != null -> {
                oauthTokenRepository.login()
                TimelineEvent.Init
            }
            else -> OauthEvent.Init
        }
        navigator.postEvent(event)
    }

    private fun updateFabVisible() {
        _isFabVisible.value = selectedItemId.value != null && fabVisible.value == true
    }

    fun setFabVisible(visible: Boolean) {
        fabVisible.value = visible
    }

    fun setSelectedItemId(selectedItemId: SelectedItemId?) {
        this.selectedItemId.value = selectedItemId
    }

    fun onFabMenuSelected(item: MenuItem) {
        Log.d("TimelineViewModel", "onFabSelected: $item")
        val selected = requireNotNull(selectedItemId.value) { "selectedItem should not be null." }
        when (item.itemId) {
            com.freshdigitable.udonroad2.timeline.R.id.iffabMenu_main_detail -> {
                navigator.postEvent(
                    TimelineEvent.TweetDetailRequested(
                        selected.quoteId ?: selected.originalId
                    )
                )
            }
        }
    }
}

@Module(
    includes = [
        TimelineFragmentModule::class,
        TweetDetailFragmentModule::class,
        MemberListListFragmentModule::class,
        OauthFragmentModule::class
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
        fun provideNavigation(
            navigator: NavigationDispatcher,
            activity: MainActivity,
            viewModelProvider: ViewModelProvider
        ): Navigation<MainActivityState> {
            return MainActivityNavigation(
                navigator,
                activity,
                viewModelProvider,
                R.id.main_container
            )
        }

        @Provides
        fun provideMainViewModel(
            navigator: NavigationDispatcher,
            oauthTokenRepository: OAuthTokenRepository
        ): MainViewModel {
            return MainViewModel(navigator, oauthTokenRepository)
        }
    }
}
