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
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.freshdigitable.udonroad2.input.databinding.FragmentTweetInputBinding
import com.freshdigitable.udonroad2.input.di.TweetInputViewModelComponent
import com.freshdigitable.udonroad2.media.MediaThumbnailContainer
import com.freshdigitable.udonroad2.media.mediaViews
import com.freshdigitable.udonroad2.model.app.AppFilePath
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class TweetInputFragment : Fragment() {
    private val args: TweetInputFragmentArgs by navArgs()

    @Inject
    lateinit var viewModelProviderFactory: TweetInputViewModelComponent.Factory
    private val viewModel: TweetInputViewModel by activityViewModels {
        viewModelProviderFactory.create(args.collapsible)
            .viewModelProviderFactory
    }

    @Inject
    internal lateinit var mediaChooserResultContract: MediaChooserResultContract
    private lateinit var mediaChooser: ActivityResultLauncher<Unit>

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        lifecycleScope.launch {
            viewModel.chooserForCameraApp.collect { onCameraAppStateChanged(it) }
        }
        mediaChooser = registerForActivityResult(mediaChooserResultContract) { uris ->
            Timber.tag("TweetInputFragment").d("mediaChooser.onResult: $uris")
            viewModel.updateMedia.dispatch(uris)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return FragmentTweetInputBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding: FragmentTweetInputBinding = DataBindingUtil.findBinding(view) ?: return
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val menuHost = requireActivity() as MenuHost
        viewModel.menuItem.observe(viewLifecycleOwner) {
            menuHost.invalidateMenu()
        }
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) =
                    menuInflater.inflate(R.menu.input_tweet_write, menu)

                override fun onPrepareMenu(menu: Menu) {
                    super.onPrepareMenu(menu)
                    val available = viewModel.menuItem.value ?: viewModel.menuItemOnIdling
                    menu.prepareItem(available)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.input_tweet_write, R.id.input_tweet_error -> {
                            viewModel.openInput.dispatch()
                            true
                        }
                        R.id.input_tweet_send -> {
                            val tweet = requireNotNull(viewModel.state.value)
                            viewModel.sendTweet.dispatch(tweet)
                            true
                        }
                        android.R.id.closeButton -> {
                            viewModel.cancelInput.dispatch()
                            true
                        }
                        else -> false
                    }
            },
            viewLifecycleOwner,
        )
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
            it.hideInputMethod()
            mediaChooser.launch(Unit)
        }
    }

    private fun onCameraAppStateChanged(state: CameraApp.State) {
        when (state) {
            is CameraApp.State.Selected -> {
                state.app.forEach {
                    requireContext().grantUriPermission(
                        it.packageName, state.path.uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
            is CameraApp.State.Finished -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    state.app.forEach {
                        requireContext().revokeUriPermission(
                            it.packageName, state.path.uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                } else {
                    requireContext().revokeUriPermission(
                        state.path.uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                MediaScannerConnection.scanFile(
                    requireContext(),
                    arrayOf(checkNotNull(state.path.file).toString()),
                    arrayOf("image/jpg"), null
                )
            }
            else -> Unit
        }
    }
}

private val Context.inputMethodManager: InputMethodManager
    get() {
        return getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

private fun View.showInputMethod() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.inputMethodManager.showSoftInput(this, 0)
    } else {
        @Suppress("DEPRECATION")
        context.inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_FORCED)
    }
}

private fun View.hideInputMethod() {
    context.inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

@BindingAdapter("bindMediaByUri")
fun MediaThumbnailContainer.bindMediaByUri(paths: Collection<AppFilePath>?) {
    if (paths == null) {
        return
    }
    mediaCount = paths.size
    paths.zip(mediaViews) { path, v ->
        v.setImageURI(path.uri)
        v.scaleType = ImageView.ScaleType.CENTER_CROP
    }
}
