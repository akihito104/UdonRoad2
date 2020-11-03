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

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.freshdigitable.udonroad2.input.databinding.FragmentTweetInputBinding
import com.freshdigitable.udonroad2.input.di.TweetInputViewModelComponent
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

class TweetInputFragment : Fragment() {
    private val args: TweetInputFragmentArgs by navArgs()
    private val mediaChooser = registerForActivityResult(MediaChooserResultContract()) {
        Timber.tag("TweetInputFragment").d("mediaChooser.onResult: $it")
    }
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it == true) {
                mediaChooser.launch(Unit)
            }
        }

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
        val binding: FragmentTweetInputBinding = DataBindingUtil.findBinding(view) ?: return
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.menuItem.observe(viewLifecycleOwner) {
            requireActivity().invalidateOptionsMenu()
        }
        binding.twIntext.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus && viewModel.isExpanded.value == false) {
                v.hideInputMethod()
            } else {
                v.showInputMethod()
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.expandAnimationEvent.collect {
                binding.twIntext.requestFocus()
            }
        }
        binding.twAppendImage.setOnClickListener {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
                ActivityCompat.checkSelfPermission(
                    it.context,
                    WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission.launch(WRITE_EXTERNAL_STORAGE)
            } else {
                mediaChooser.launch(Unit)
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

private val Context.inputMethodManager: InputMethodManager
    get() {
        return getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

private fun View.showInputMethod() {
    context.inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_FORCED)
}

private fun View.hideInputMethod() {
    context.inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}
