package com.freshdigitable.udonroad2.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.databinding.ActivityUserBinding
import com.freshdigitable.udonroad2.di.UserViewModelComponent
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import kotlin.math.abs

class UserActivity : HasAndroidInjector, AppCompatActivity() {
    @Inject
    lateinit var userViewModelComponentFactory: UserViewModelComponent.Factory
    private val viewModel: UserViewModel by viewModels {
        userViewModelComponentFactory.create(user).viewModelProviderFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityUserBinding>(
            this,
            R.layout.activity_user
        )
        val adapter = UserFragmentPagerAdapter(supportFragmentManager)

        binding.setup(viewModel, adapter)
        setSupportActionBar(binding.userToolbar)
        viewModel.relationshipMenuItems.observe(this) {
            invalidateOptionsMenu()
        }
        lifecycleScope.launchWhenCreated {
            viewModel.pages.collect {
                adapter.setItems(it)
                adapter.notifyDataSetChanged()
            }
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
            viewModel.setCurrentPage(currentItem)
        }
        userTabContainer.setupWithViewPager(userPager)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_user_relation, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val availableItems = viewModel.relationshipMenuItems.value ?: return false
        RelationshipMenu.values().forEach { menu.findItem(it.id).isVisible = false }
        availableItems.forEach { menu.findItem(it.id).isVisible = true }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (viewModel.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private val args: UserActivityArgs by lazy {
        UserActivityArgs.fromBundle(requireNotNull(intent.extras))
    }
    private val user: TweetUserItem get() = args.user

    companion object {
        fun getIntent(context: Context, user: TweetUserItem): Intent {
            val intent = Intent(context, UserActivity::class.java)
            intent.putExtras(UserActivityArgs(user).toBundle())
            return intent
        }

        fun start(context: Context, user: TweetUserItem) {
            val intent = getIntent(context, user)
            context.startActivity(intent)
        }
    }

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = injector
}

@BindingAdapter("updateTabTexts")
fun TabLayout.updateText(user: UserEntity?) {
    for (index in UserPage.values().indices) {
        val tab = getTabAt(index)
        val page = UserPage.values()[index]
        tab?.text = when {
            page.count != null -> context.getString(page.titleRes, page.count.invoke(user) ?: "---")
            else -> context.getString(page.titleRes)
        }
    }
}
