package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem

interface ListItemClickListener<I> {
    fun onBodyItemClicked(item: I) {}
    fun onUserIconClicked(user: TweetUserItem) {}
}

interface TweetListItemClickListener : ListItemClickListener<TweetListItem> {
    fun onQuoteItemClicked(item: TweetListItem) {}

    fun onMediaItemClicked(originalId: TweetId, item: TweetElement, index: Int) {
        onMediaItemClicked(originalId, null, item, index)
    }

    fun onMediaItemClicked(originalId: TweetId, quotedId: TweetId?, item: TweetElement, index: Int)
}

interface TweetListEventListener {
    val selectedItemId: LiveData<SelectedItemId?>
}
