package com.freshdigitable.udonroad2.timeline

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.freshdigitable.udonroad2.timeline.databinding.FragmentDetailBinding
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class TweetDetailFragment : Fragment() {

    private lateinit var binding: FragmentDetailBinding
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        val viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(TweetDetailViewModel::class.java)
        binding.viewModel = viewModel

        viewModel.tweetItem.observe(viewLifecycleOwner, Observer { item ->
            binding.detailReactionContainer.removeAllViews()
            item?.body?.retweetCount?.let {
                AppCompatTextView(view.context).apply {
                    text = "RT: $it"
                }
            }?.let {
                binding.detailReactionContainer.addView(it, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    rightMargin = 10 // XXX
                })
            }
            item?.body?.favoriteCount?.let {
                AppCompatTextView(view.context).apply {
                    text = "fav: $it"
                }
            }?.let {
                binding.detailReactionContainer.addView(it, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            }
        })

        viewModel.showTweetItem(getTweetId())
    }

    private fun getTweetId(): Long {
        return requireNotNull(arguments?.getLong(ARGS_TWEET_ID)) { "use TweetDetailFragment.newInstance()" }
    }

    companion object {
        private const val ARGS_TWEET_ID = "tweet_id"

        fun newInstance(tweetId: Long): TweetDetailFragment {
            val args = Bundle().apply {
                putLong(ARGS_TWEET_ID, tweetId)
            }
            return TweetDetailFragment().apply {
                arguments = args
            }
        }
    }
}
