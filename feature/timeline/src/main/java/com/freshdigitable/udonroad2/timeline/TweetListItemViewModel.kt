package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem

interface ListItemClickListener<I> {
    fun onBodyItemClicked(item: I) {}
    fun onUserIconClicked(user: TweetUserItem) {}
}

interface TweetListItemEventListener : ListItemClickListener<TweetListItem> {
    override fun onBodyItemClicked(item: TweetListItem)
    override fun onUserIconClicked(user: TweetUserItem)
    fun onQuoteItemClicked(item: TweetListItem)
}

interface TweetListItemViewModel : TweetListItemEventListener {
    val selectedItemId: LiveData<SelectedItemId?>
}
