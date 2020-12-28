package com.freshdigitable.udonroad2.timeline.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.freshdigitable.udonroad2.shortcut.TweetDetailContextMenuView
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.databinding.FragmentDetailBinding
import com.freshdigitable.udonroad2.timeline.di.TweetDetailViewModelComponent
import com.freshdigitable.udonroad2.timeline.viewmodel.TweetDetailViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.TweetDetailViewStates
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class TweetDetailFragment : Fragment() {
    private val args: TweetDetailFragmentArgs by navArgs()

    @Inject
    lateinit var viewModelComponentFactory: TweetDetailViewModelComponent.Factory

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentDetailBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DataBindingUtil.findBinding<FragmentDetailBinding>(view) ?: return
        binding.lifecycleOwner = viewLifecycleOwner

        val component = viewModelComponentFactory.create(args.tweetId)
        val viewModel = component.viewModel(this)
        binding.viewModel = viewModel

        viewModel.tweetItem.observe(viewLifecycleOwner) { item ->
            binding.detailReactionContainer.removeAllViews()
            item?.body?.retweetCount?.let {
                AppCompatTextView(view.context).apply {
                    text = "RT: $it"
                }
            }?.let {
                binding.detailReactionContainer.addView(
                    it,
                    LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        rightMargin = 10 // XXX
                    }
                )
            }
            item?.body?.favoriteCount?.let {
                AppCompatTextView(view.context).apply {
                    text = "fav: $it"
                }
            }?.let {
                binding.detailReactionContainer.addView(
                    it,
                    LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                )
            }
        }

        val eventDelegate = component.activityEventDelegate
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigationEvent.collect(eventDelegate::dispatchNavHostNavigate)
        }
    }

    companion object {
        private fun TweetDetailViewModelComponent.viewModel(
            owner: ViewModelStoreOwner
        ): TweetDetailViewModel {
            val viewModelProvider = ViewModelProvider(owner, viewModelProviderFactory)
            return viewModelProvider[TweetDetailViewModel::class.java]
        }
    }
}

@BindingAdapter("menuItemState")
fun TweetDetailContextMenuView.updateMenuItemState(item: TweetDetailViewStates.MenuItemState?) {
    updateMenuItem {
        changeGroupEnabled(R.id.menuGroup_detailMain, item?.isMainGroupEnabled ?: false)
        onMenuItem(R.id.detail_main_rt) {
            isChecked = item?.isRetweetChecked ?: false
        }
        onMenuItem(R.id.detail_main_fav) {
            isChecked = item?.isFavChecked ?: false
        }
        onMenuItem(R.id.detail_more_delete) {
            isVisible = item?.isDeleteVisible ?: false
        }
    }
}
