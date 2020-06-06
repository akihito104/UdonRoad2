package com.freshdigitable.udonroad2.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.observe
import androidx.viewpager.widget.ViewPager
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.databinding.ActivityUserBinding
import com.freshdigitable.udonroad2.model.TweetingUser
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.navigation.Navigation
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.timeline.fragment.MemberListListFragmentModule
import com.freshdigitable.udonroad2.timeline.fragment.TimelineFragmentModule
import com.freshdigitable.udonroad2.timeline.fragment.UserListFragmentModule
import com.google.android.material.appbar.AppBarLayout
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlin.math.abs

class UserActivity : HasAndroidInjector, AppCompatActivity() {
    @Inject
    lateinit var viewModelProvider: ViewModelProvider
    @Inject
    lateinit var navigation: Navigation<UserActivityState>
    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding = setContentView<ActivityUserBinding>(
            this,
            R.layout.activity_user
        )
        viewModel = viewModelProvider[UserViewModel::class.java]
        val adapter =
            UserFragmentPagerAdapter(supportFragmentManager, user)

        binding.setup(viewModel, adapter)
        viewModel.setUserId(user.id)
        binding.userToolbar.title = ""
        setSupportActionBar(binding.userToolbar)
    }

    private fun ActivityUserBinding.setup(
        viewModel: UserViewModel,
        adapter: UserFragmentPagerAdapter
    ) {
        lifecycleOwner = this@UserActivity

        userAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBar, offset ->
            viewModel.setAppBarScrollRate(abs(offset).toFloat() / appBar.totalScrollRange.toFloat())
        })

        viewModel.user.observe(this@UserActivity) { u ->
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
        }
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
        viewModel.relationship.observe(this@UserActivity) {
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_user_relation, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val r = viewModel.relationship.value ?: return false
        menu?.run {
            switchVisibility(
                R.id.action_follow,
                R.id.action_unfollow, !r.following
            )
            switchVisibility(
                R.id.action_block,
                R.id.action_unblock, !r.blocking
            )
            switchVisibility(
                R.id.action_mute,
                R.id.action_unmute, !r.muting
            )
            switchVisibility(
                R.id.action_block_retweet,
                R.id.action_unblock_retweet,
                if (r.following) r.wantRetweets else null
            )
        }
        return true
    }

    private fun Menu.switchVisibility(
        @IdRes positiveId: Int,
        @IdRes negativeId: Int,
        positiveItemVisible: Boolean?
    ) {
        findItem(positiveId).isVisible = positiveItemVisible ?: false
        findItem(negativeId).isVisible = positiveItemVisible?.not() ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_follow -> viewModel.updateFollowingStatus(true)
            R.id.action_unfollow -> viewModel.updateFollowingStatus(false)
            R.id.action_block -> viewModel.updateBlockingStatus(true)
            R.id.action_unblock -> viewModel.updateBlockingStatus(false)
            R.id.action_mute -> viewModel.updateMutingStatus(true)
            R.id.action_unmute -> viewModel.updateMutingStatus(false)
            R.id.action_block_retweet -> viewModel.updateWantRetweet(false)
            R.id.action_unblock_retweet -> viewModel.updateWantRetweet(true)
            R.id.action_r4s -> viewModel.reportForSpam()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private val user: TweetingUser
        get() = intent.getSerializableExtra(ARGS_USER) as TweetingUser

    companion object {
        private const val ARGS_USER = "user"

        fun start(context: Context, user: TweetingUser) {
            val intent = Intent(context, UserActivity::class.java)
            intent.putExtra(ARGS_USER, user)
            context.startActivity(intent)
        }
    }

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = injector
}

@Module(
    includes = [
        TimelineFragmentModule::class,
        UserViewModelModule::class,
        UserListFragmentModule::class,
        MemberListListFragmentModule::class
    ]
)
abstract class UserActivityModule {
    @Binds
    @IntoMap
    @ViewModelKey(UserViewModel::class)
    abstract fun bindUserViewModel(viewModel: UserViewModel): ViewModel

    @Binds
    abstract fun bindViewModelStoreOwner(activity: UserActivity): ViewModelStoreOwner

    @Module
    companion object {
        @Provides
        fun provideUserActivityNavigation(
            navigator: NavigationDispatcher,
            activity: UserActivity,
            viewModelProvider: ViewModelProvider
        ): Navigation<UserActivityState> {
            return UserActivityNavigation(
                navigator,
                activity,
                0,
                viewModelProvider
            )
        }
    }
}
