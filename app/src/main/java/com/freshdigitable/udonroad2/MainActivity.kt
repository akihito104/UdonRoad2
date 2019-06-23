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

package com.freshdigitable.udonroad2

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.freshdigitable.udonroad2.databinding.ActivityMainBinding
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.navigation.Navigation
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.timeline.MemberListListFragmentModule
import com.freshdigitable.udonroad2.timeline.SelectedItemId
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TimelineFragmentModule
import com.freshdigitable.udonroad2.timeline.TweetDetailFragment
import com.freshdigitable.udonroad2.timeline.TweetDetailViewModel
import com.freshdigitable.udonroad2.timeline.TweetDetailViewModelModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import dagger.multibindings.IntoMap
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var navigation: Navigation<MainActivityState>
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        navigation.navigator.postEvent(TimelineEvent.Init)

        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
    }

    override fun onBackPressed() {
        navigation.navigator.postEvent(TimelineEvent.Back)
    }

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = androidInjector
}

class MainViewModel @Inject constructor(
    private val navigator: NavigationDispatcher
) : ViewModel() {

    private val selectedItemId = MutableLiveData<SelectedItemId?>()
    private val fabVisible = MutableLiveData<Boolean>()
    private val _isFabVisible = MediatorLiveData<Boolean>()
    val isFabVisible: LiveData<Boolean> = _isFabVisible

    init {
        _isFabVisible.addSource(selectedItemId) { updateFabVisible() }
        _isFabVisible.addSource(fabVisible) { updateFabVisible() }
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
        TweetDetailViewModelModule::class,
        MemberListListFragmentModule::class
    ]
)
abstract class MainActivityModule {
    @FragmentScope
    @ContributesAndroidInjector
    abstract fun contributeTweetDetailFragment(): TweetDetailFragment

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TweetDetailViewModel::class)
    abstract fun bindTweetDetailViewModel(viewModel: TweetDetailViewModel): ViewModel

    @Module
    companion object {
        @Provides
        @JvmStatic
        fun provideNavigation(
            navigator: NavigationDispatcher,
            activity: MainActivity,
            viewModelFactory: ViewModelProvider.Factory
        ): Navigation<MainActivityState> {
            return MainActivityNavigation(
                navigator,
                activity,
                viewModelFactory,
                R.id.main_container
            )
        }
    }
}
