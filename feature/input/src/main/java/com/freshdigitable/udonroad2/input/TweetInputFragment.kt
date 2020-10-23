/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.input

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.freshdigitable.udonroad2.input.databinding.FragmentTweetInputBinding
import com.freshdigitable.udonroad2.input.di.TweetInputViewModelComponent
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class TweetInputFragment : Fragment() {
    private val args: TweetInputFragmentArgs by navArgs()

    @Inject
    lateinit var viewModelProviderFactory: TweetInputViewModelComponent.Factory
    private val viewModel: TweetInputViewModel by activityViewModels {
        viewModelProviderFactory.create(args.collapsible)
            .viewModelProviderFactory
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentTweetInputBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DataBindingUtil.findBinding<FragmentTweetInputBinding>(view)?.apply { // FIXME
            twName.text = "test"
            twIcon.setImageResource(R.drawable.ic_like)
        } ?: return
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.menuItem.observe(viewLifecycleOwner) {
            requireActivity().invalidateOptionsMenu()
        }
        binding.twIntext.setOnFocusChangeListener { v, hasFocus ->
            val inputMethod =
                v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (!hasFocus && viewModel.isExpanded.value == false) {
                inputMethod.hideSoftInputFromWindow(v.windowToken, 0)
            } else {
                inputMethod.showSoftInput(v, InputMethodManager.SHOW_FORCED)
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.expandAnimationEvent.collect {
                binding.twIntext.requestFocus()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.input_tweet_write, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val available = viewModel.menuItem.value ?: return
        menu.prepareItem(available)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.input_tweet_write -> {
                viewModel.onWriteClicked()
                true
            }
            R.id.input_tweet_send -> {
                viewModel.onSendClicked()
                true
            }
            android.R.id.closeButton -> {
                viewModel.onCancelClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

enum class InputMenuItem(
    val itemId: Int,
    val enabled: Boolean
) {
    WRITE_ENABLED(R.id.input_tweet_write, true),
    WRITE_DISABLED(R.id.input_tweet_write, false),
    SEND_ENABLED(R.id.input_tweet_send, true),
    SEND_DISABLED(R.id.input_tweet_send, false),
    RETRY_ENABLED(R.id.input_tweet_error, true),
}

fun Menu.prepareItem(available: InputMenuItem) {
    InputMenuItem.values().map { it.itemId }.distinct().forEach {
        val item = findItem(it)
        when (item.itemId) {
            available.itemId -> {
                item.isVisible = true
                item.isEnabled = available.enabled
            }
            else -> {
                item.isVisible = false
            }
        }
    }
}

@BindingAdapter("isExpanded", "onExpandAnimationEnd", requireAll = false)
fun View.expand(isExpanded: Boolean?, onExpandAnimationEnd: (() -> Unit)?) {
    when (isExpanded) {
        true -> setupExpendAnim(onExpandAnimationEnd)
        else -> collapseWithAnim()
    }
}

private fun View.setupExpendAnim(onExpandAnimEnd: (() -> Unit)?) {
    measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    when {
        measuredHeight > 0 -> expandWithAnim(onExpandAnimEnd)
        else -> doOnPreDraw { it.expandWithAnim(onExpandAnimEnd) }
    }
}

private fun View.expandWithAnim(onExpandAnimEnd: (() -> Unit)?) {
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
            onExpandAnimEnd?.invoke()
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
