package com.freshdigitable.udonroad2.timeline.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.freshdigitable.udonroad2.model.TwitterCard
import com.freshdigitable.udonroad2.model.tweet.DetailTweetListItem
import com.freshdigitable.udonroad2.timeline.databinding.FragmentDetailBinding
import com.freshdigitable.udonroad2.timeline.di.TweetDetailViewModelComponent
import com.freshdigitable.udonroad2.timeline.viewmodel.SpanClickListener
import com.freshdigitable.udonroad2.timeline.viewmodel.TweetDetailViewModel
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
        savedInstanceState: Bundle?,
    ): View = FragmentDetailBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DataBindingUtil.findBinding<FragmentDetailBinding>(view) ?: return
        binding.lifecycleOwner = viewLifecycleOwner

        val component = viewModelComponentFactory.create(args.tweetId)
        val viewModel = component.viewModel(this)
        binding.viewModel = viewModel

        viewModel.state.map { it.tweetItem }.observe(viewLifecycleOwner) { item ->
            binding.detailReactionContainer.removeAllViews()
            if (item?.body?.retweetCount != null) {
                binding.addReaction("RT", item.body.retweetCount)
            }
            if (item?.body?.favoriteCount != null) {
                binding.addReaction("fav", item.body.favoriteCount)
            }
        }

        val eventDelegate = component.activityEffectDelegate
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effect.collect(eventDelegate::accept)
        }
    }

    companion object {
        private fun TweetDetailViewModelComponent.viewModel(
            owner: ViewModelStoreOwner,
        ): TweetDetailViewModel {
            val viewModelProvider = ViewModelProvider(owner, viewModelProviderFactory)
            return viewModelProvider[TweetDetailViewModel::class.java]
        }

        @SuppressLint("SetTextI18n")
        private fun FragmentDetailBinding.addReaction(
            type: String,
            count: Int,
        ) {
            val iconAttachedTextView = AppCompatTextView(root.context).apply {
                text = "$type: $count"
            }
            val lp = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                rightMargin = 10 // XXX
            }
            detailReactionContainer.addView(iconAttachedTextView, lp)
        }
    }
}

@BindingAdapter("textWithLinkableUrl", "spanClickListener", requireAll = true)
fun TextView.makeUrlLinkable(
    tweetItem: DetailTweetListItem?,
    spanClickListener: SpanClickListener?,
) {
    if (tweetItem == null) {
        text = ""
        return
    }
    if (spanClickListener == null || tweetItem.body.urlItems.isEmpty()) {
        text = tweetItem.bodyTextWithDisplayUrl
        return
    }

    val bodyText = tweetItem.bodyTextWithDisplayUrl
    movementMethod = LinkMovementMethod.getInstance()
    text = tweetItem.body.urlItems.fold(SpannableStringBuilder(bodyText)) { t, u ->
        val start = bodyText.indexOf(u.displayUrl)
        val span = when {
            start < 0 -> return@fold t
            start >= 0 -> {
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        spanClickListener.onSpanClicked(u)
                    }
                }
            }
            else -> throw AssertionError()
        }
        t.apply {
            setSpan(span, start, start + u.displayUrl.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

@BindingAdapter("cardImage")
fun ImageView.bindCardImage(card: TwitterCard?) {
    if (card == null) {
        return
    }
    Glide.with(this)
        .load(card.imageUrl)
        .into(this)
}
