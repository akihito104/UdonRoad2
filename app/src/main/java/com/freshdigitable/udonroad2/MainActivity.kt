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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.navigation.Navigation
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TimelineFragment
import com.freshdigitable.udonroad2.timeline.TimelineViewModel
import com.freshdigitable.udonroad2.timeline.TimelineViewModelModule
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

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.navigator.postEvent(TimelineEvent.Init)
    }

    override fun onBackPressed() {
        navigation.navigator.postEvent(TimelineEvent.Back)
    }

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Fragment>
    override fun supportFragmentInjector(): AndroidInjector<Fragment> = androidInjector
}

@Module(includes = [
    TimelineViewModelModule::class
])
abstract class MainActivityModule {
    @FragmentScope
    @ContributesAndroidInjector
    abstract fun contributeTimelineFragment(): TimelineFragment

    @Binds
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    abstract fun bindMainViewModel(viewModel: TimelineViewModel): ViewModel

    @Module
    companion object {
        @Provides
        @JvmStatic
        fun provideNavigation(
            navigator: NavigationDispatcher,
            activity: MainActivity
        ): Navigation<MainActivityState> {
            return MainActivityNavigation(navigator, activity, R.id.main_container)
        }
    }
}
