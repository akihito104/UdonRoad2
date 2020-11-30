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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.shortcut.TweetDetailContextMenuView
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.databinding.FragmentDetailBinding
import com.freshdigitable.udonroad2.timeline.di.TweetDetailViewModelComponent
import com.freshdigitable.udonroad2.timeline.viewmodel.TweetDetailViewModel
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class TweetDetailFragment : Fragment() {
    private val args: TweetDetailFragmentArgs by navArgs()
    private lateinit var binding: FragmentDetailBinding

    @Inject
    lateinit var viewModelComponentFactory: TweetDetailViewModelComponent.Factory
    private val viewModel: TweetDetailViewModel by viewModels {
        viewModelComponentFactory.create(args.tweetId).viewModelProviderFactory
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
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
    }
}

@BindingAdapter("menuItemState")
fun TweetDetailContextMenuView.updateMenuItemState(item: TweetListItem?) {
    updateMenuItem {
        onMenuItem(R.id.detail_main_rt) {
            isEnabled = item != null
            isChecked = item?.body?.isRetweeted ?: false
        }
        onMenuItem(R.id.detail_main_fav) {
            isEnabled = item != null
            isChecked = item?.body?.isFavorited ?: false
        }
        onMenuItem(R.id.detail_main_conv) {
            isEnabled = item != null
        }
        onMenuItem(R.id.detail_main_quote) {
            isEnabled = item != null
        }
        onMenuItem(R.id.detail_main_reply) {
            isEnabled = item != null
        }
    }
}
