package com.freshdigitable.udonroad2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.freshdigitable.udonroad2.databinding.ActivityUserBinding
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.timeline.TimelineFragment
import com.freshdigitable.udonroad2.timeline.TimelineViewModel
import com.freshdigitable.udonroad2.timeline.TimelineViewModelModule
import com.google.android.material.appbar.AppBarLayout
import dagger.Binds
import dagger.Module
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

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding = setContentView<ActivityUserBinding>(this, R.layout.activity_user)
        binding.lifecycleOwner = this

        binding.userPager.adapter = object : FragmentStatePagerAdapter(
            supportFragmentManager, RESUME_ONLY_CURRENT_FRAGMENT) {
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

@Module(includes = [
    TimelineViewModelModule::class,
    UserViewModelModule::class
])
interface UserActivityModule {
    @ContributesAndroidInjector
    @FragmentScope
    fun contributeTimelineFragment(): TimelineFragment

    @Binds
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    fun bindTimelineFragment(viewModel: TimelineViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserViewModel::class)
    fun bindUserViewModel(viewModel: UserViewModel): ViewModel
}
