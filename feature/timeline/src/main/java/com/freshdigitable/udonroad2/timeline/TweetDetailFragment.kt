package com.freshdigitable.udonroad2.timeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class TweetDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    companion object {
        fun newInstance(tweetId: Long): TweetDetailFragment {
            val args = Bundle().apply {
                putLong("tweet_id", tweetId)
            }
            return TweetDetailFragment().apply {
                arguments = args
            }
        }
    }
}
