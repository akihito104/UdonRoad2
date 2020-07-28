package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.Tweet
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.model.TweetingUser

interface ListItemClickListener<I> {
    fun onBodyItemClicked(item: I) {}
    fun onUserIconClicked(user: TweetingUser) {}
}

interface TweetListItemClickListener : ListItemClickListener<TweetListItem> {
    fun onQuoteItemClicked(item: TweetListItem) {}
    fun onMediaItemClicked(originalId: Long, item: Tweet, index: Int)
}

interface TweetListEventListener {
    val selectedItemId: LiveData<SelectedItemId?>
}
