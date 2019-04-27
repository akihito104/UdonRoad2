package com.freshdigitable.udonroad2.timeline

import com.freshdigitable.udonroad2.model.TweetListItem

interface TweetListItemClickListener {
    fun onBodyItemClicked(item: TweetListItem)

    fun onQuoteItemClicked(item: TweetListItem)

    fun onUserIconClicked(item: TweetListItem)
}
