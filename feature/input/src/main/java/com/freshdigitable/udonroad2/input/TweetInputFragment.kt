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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.freshdigitable.udonroad2.input.databinding.FragmentTweetInputBinding
import com.freshdigitable.udonroad2.input.di.TweetInputViewModelComponent
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class TweetInputFragment : Fragment() {
    private val args: TweetInputFragmentArgs by navArgs()

    @Inject
    lateinit var viewModelProviderFactory: TweetInputViewModelComponent.Factory
    private val viewModel: TweetInputViewModel by viewModels {
        viewModelProviderFactory.create(args.collapsable)
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

        binding.twIntext.doAfterTextChanged {
            viewModel.onTweetTextChanged(it?.toString() ?: "")
        }
        viewModel.text.observe(viewLifecycleOwner) {
            if (binding.twIntext.text.toString() != it) {
                binding.twIntext.setText(it)
            }
        }
        viewModel.menuItem.observe(viewLifecycleOwner) {
            requireActivity().invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.input_tweet_write, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val available = viewModel.menuItem.value ?: return
        InputMenuItem.values().map { it.itemId }.distinct().forEach {
            val item = menu.findItem(it)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.input_tweet_send -> {
                viewModel.onSendClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

enum class TweetInputState { IDLING, OPENED, SENDING, SUCCEEDED, FAILED }

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

fun TweetInputState.toMenuItem(): InputMenuItem {
    return when (this) {
        TweetInputState.IDLING -> InputMenuItem.WRITE_ENABLED
        TweetInputState.OPENED -> InputMenuItem.SEND_ENABLED
        TweetInputState.SENDING -> InputMenuItem.SEND_DISABLED
        TweetInputState.SUCCEEDED -> InputMenuItem.WRITE_DISABLED
        TweetInputState.FAILED -> InputMenuItem.RETRY_ENABLED
    }
}

sealed class TweetInputEvent : AppEvent {
    object Open : TweetInputEvent()
    object Close : TweetInputEvent()
    object Send : TweetInputEvent()
}
