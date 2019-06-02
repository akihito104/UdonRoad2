package com.freshdigitable.udonroad2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.freshdigitable.udonroad2.databinding.ActivityUserBinding
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.navigation.Navigation
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.navigation.NavigationEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TimelineFragment
import com.freshdigitable.udonroad2.timeline.TimelineViewModel
import com.freshdigitable.udonroad2.timeline.TimelineViewModelModule
import com.google.android.material.appbar.AppBarLayout
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

class UserActivity : HasSupportFragmentInjector, AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var navigation: Navigation<UserActivityState>

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding = setContentView<ActivityUserBinding>(this, R.layout.activity_user)
        binding.lifecycleOwner = this

        binding.userPager.adapter = object : FragmentStatePagerAdapter(
            supportFragmentManager, RESUME_ONLY_CURRENT_FRAGMENT
        ) {
            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> TimelineFragment.newInstance(ListQuery.Timeline(userId))
                    1 -> TimelineFragment.newInstance(ListQuery.Fav(userId))
                    else -> throw IllegalStateException()
                }
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return when (position) {
                    0 -> "tweet"
                    1 -> "fav"
                    else -> throw IllegalStateException()
                }
            }

            override fun getCount(): Int = 2
        }

        binding.userTabContainer.setupWithViewPager(binding.userPager)

        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(UserViewModel::class.java)
        binding.viewModel = viewModel
        viewModel.setUserId(userId)

        binding.userAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBar, offset ->
            viewModel.setAppBarScrollRate(Math.abs(offset).toFloat() / appBar.totalScrollRange.toFloat())
        })
        binding.userPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                viewModel.setCurrentPage(position)
            }
        })
        viewModel.fabVisible.observe(this, Observer {
            if (it == true) {
                binding.userFab.show()
            } else {
                binding.userFab.hide()
            }
        })
    }

    private val userId: Long
        get() = intent.getLongExtra(ARGS_USER_ID, -1)

    companion object {
        private const val ARGS_USER_ID = "user_id"

        fun start(context: Context, userId: Long) {
            val intent = Intent(context, UserActivity::class.java)
            intent.putExtra(ARGS_USER_ID, userId)
            context.startActivity(intent)
        }
    }

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = injector
}

class UserActivityState : FragmentContainerState

class UserActivityNavigation(
    navigator: NavigationDispatcher,
    activity: UserActivity,
    @IdRes containerId: Int,
    viewModelFactory: ViewModelProvider.Factory
) : Navigation<UserActivityState>(navigator, activity, containerId) {

    private val viewModel =
        ViewModelProviders.of(activity, viewModelFactory).get(UserViewModel::class.java)

    override fun onEvent(event: NavigationEvent): UserActivityState? {
        if (event is TimelineEvent.TweetItemSelected) {
            viewModel.setSelectedItemId(event.selectedItemId)
        }
        return null
    }

    override fun navigate(s: UserActivityState?) {}
}

@Module(
    includes = [
        TimelineViewModelModule::class,
        UserViewModelModule::class
    ]
)
abstract class UserActivityModule {
    @ContributesAndroidInjector
    @FragmentScope
    abstract fun contributeTimelineFragment(): TimelineFragment

    @Binds
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    abstract fun bindTimelineFragment(viewModel: TimelineViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserViewModel::class)
    abstract fun bindUserViewModel(viewModel: UserViewModel): ViewModel

    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideUserActivityNavigation(
            navigator: NavigationDispatcher,
            activity: UserActivity,
            viewModelFactory: ViewModelProvider.Factory
        ): Navigation<UserActivityState> {
            return UserActivityNavigation(navigator, activity, 0, viewModelFactory)
        }
    }
}
