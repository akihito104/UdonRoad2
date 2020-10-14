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

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.databinding.ActivityMainBinding
import com.freshdigitable.udonroad2.input.R.id
import com.freshdigitable.udonroad2.input.TweetInputFragment
import com.freshdigitable.udonroad2.input.TweetInputFragmentArgs
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    lateinit var navigation: MainActivityNavigationDelegate

    @Inject
    lateinit var viewModelProviderFactory: ViewModelProvider.Factory
    private val viewModel: MainViewModel by viewModels { viewModelProviderFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = findViewById<View>(R.id.main_drawer)?.let {
            DataBindingUtil.findBinding(it)
        } ?: DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setSupportActionBar(binding.mainToolbar)

        viewModel.initialEvent(savedInstanceState?.savedViewState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        supportFragmentManager.commit {
            add<TweetInputFragment>(
                binding.mainInputContainer.id,
                args = TweetInputFragmentArgs(false).toBundle()
            )
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.input_tweet_write, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.input_tweet_write -> {
                val view = supportFragmentManager.findFragmentById(R.id.main_inputContainer)?.view
                    ?: return false
                view.setupExpendAnim()
                startSupportActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        mode?.setTitle(R.string.title_input_send_tweet)
                        mode?.menuInflater?.inflate(R.menu.input_tweet_send, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean =
                        false

                    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                        return when (item?.itemId) {
                            id.input_tweet_send -> {
                                // TODO: send tweet
                                mode?.finish()
                                true
                            }
                            else -> false
                        }
                    }

                    override fun onDestroyActionMode(mode: ActionMode?) {
                        view.collapseWithAnim()
                    }
                })
                true
            }
            else -> super.onOptionsItemSelected(item)
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

fun View.setupExpendAnim() {
    measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    when {
        measuredHeight > 0 -> expandWithAnim()
        else -> doOnPreDraw { it.expandWithAnim() }
    }
}

fun View.expandWithAnim() {
    val container = parent as View
    val h = measuredHeight
    ValueAnimator.ofInt(-h, 0).apply {
        duration = 200
        interpolator = FastOutSlowInInterpolator()
        doOnStart {
            this@expandWithAnim.visibility = View.VISIBLE
        }
        addUpdateListener {
            val animValue = it.animatedValue as Int
            this@expandWithAnim.translationY = animValue.toFloat()
            container.updateLayoutParams {
                height = h + animValue
            }
        }
        doOnEnd {
            this@expandWithAnim.translationY = 0f
            container.updateLayoutParams {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }.start()
}

fun View.collapseWithAnim() {
    val container = parent as View
    val h = measuredHeight
    ValueAnimator.ofInt(-h).apply {
        duration = 200
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener {
            val animValue = it.animatedValue as Int
            this@collapseWithAnim.translationY = animValue.toFloat()
            container.updateLayoutParams {
                height = h + animValue
            }
        }
        doOnEnd {
            this@collapseWithAnim.visibility = View.GONE
        }
    }.start()
}
