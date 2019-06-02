package com.freshdigitable.udonroad2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.freshdigitable.udonroad2.databinding.ActivityUserBinding
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.navigation.Navigation
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
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
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(UserViewModel::class.java)
        val adapter = UserFragmentPagerAdapter(supportFragmentManager, userId)

        binding.setup(viewModel, adapter)
        viewModel.setUserId(userId)
    }

    private fun ActivityUserBinding.setup(
        viewModel: UserViewModel,
        adapter: UserFragmentPagerAdapter
    ) {
        lifecycleOwner = this@UserActivity

        userAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBar, offset ->
            viewModel.setAppBarScrollRate(Math.abs(offset).toFloat() / appBar.totalScrollRange.toFloat())
        })

        viewModel.user.observe(this@UserActivity, Observer { u ->
            adapter.apply {
                titles.clear()
                titles.addAll(UserPage.values().map { p ->
                    if (p.count != null) {
                        this@UserActivity.getString(p.titleRes, p.count.invoke(u) ?: "---")
                    } else {
                        this@UserActivity.getString(p.titleRes)
                    }
                })
            }.notifyDataSetChanged()
        })
        userPager.apply {
            this.adapter = adapter
            addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    viewModel.setCurrentPage(position)
                }
            })
        }

        userTabContainer.setupWithViewPager(userPager)

        this.viewModel = viewModel
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
