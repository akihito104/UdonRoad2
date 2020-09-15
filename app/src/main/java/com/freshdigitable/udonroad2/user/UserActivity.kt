package com.freshdigitable.udonroad2.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.observe
import androidx.viewpager.widget.ViewPager
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.databinding.ActivityUserBinding
import com.freshdigitable.udonroad2.di.ListItemFragmentModule
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject
import kotlin.math.abs

class UserActivity : HasAndroidInjector, AppCompatActivity() {
    @Inject
    lateinit var userViewModelComponentFactory: UserViewModelComponent.Factory

    private val viewModel: UserViewModel by lazy {
        userViewModelComponentFactory.create(user)
            .viewModelProvider[UserViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityUserBinding>(
            this,
            R.layout.activity_user
        )
        val adapter = UserFragmentPagerAdapter(supportFragmentManager, viewModel)

        binding.setup(viewModel, adapter)
        setSupportActionBar(binding.userToolbar)
        viewModel.relationship.observe(this) {
            invalidateOptionsMenu()
        }
    }

    private fun ActivityUserBinding.setup(
        viewModel: UserViewModel,
        adapter: UserFragmentPagerAdapter
    ) {
        lifecycleOwner = this@UserActivity
        this.viewModel = viewModel

        userToolbar.title = ""
        userAppBar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBar, offset ->
                viewModel.setAppBarScrollRate(
                    abs(offset).toFloat() / appBar.totalScrollRange.toFloat()
                )
            }
        )

        userPager.apply {
            addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    viewModel.setCurrentPage(position)
                }
            })
            this.adapter = adapter
            viewModel.setCurrentPage(userPager.currentItem)
        }
        userTabContainer.setupWithViewPager(userPager)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private val args: UserActivityArgs by lazy {
        UserActivityArgs.fromBundle(requireNotNull(intent.extras))
    }
    private val user: TweetingUser get() = args.user

    companion object {
        fun getIntent(context: Context, user: TweetingUser): Intent {
            val intent = Intent(context, UserActivity::class.java)
            intent.putExtras(UserActivityArgs(user).toBundle())
            return intent
        }

        fun start(context: Context, user: TweetingUser) {
            val intent = getIntent(context, user)
            context.startActivity(intent)
        }
    }

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = injector
}

@BindingAdapter("updateTabTexts")
fun TabLayout.updateText(user: User?) {
    for (index in UserPage.values().indices) {
        val tab = getTabAt(index)
        val page = UserPage.values()[index]
        tab?.text = when {
            page.count != null -> context.getString(page.titleRes, page.count.invoke(user) ?: "---")
            else -> context.getString(page.titleRes)
        }
    }
}

@Module(
    includes = [
        ListItemFragmentModule::class,
        UserViewModelComponentModule::class
    ]
)
interface UserActivityModule {
    @Binds
    fun bindViewModelStoreOwner(activity: UserActivity): ViewModelStoreOwner

    @Binds
    fun bindActivityEventDelegate(eventDelegate: UserActivityNavigationDelegate): ActivityEventDelegate

    @Module
    companion object {
        @Provides
        fun provideActivityEventDelegate(activity: UserActivity): UserActivityNavigationDelegate {
            return UserActivityNavigationDelegate(activity)
        }
    }
}
