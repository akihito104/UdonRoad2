package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem

interface ListItemClickListener<I> {
    fun onBodyItemClicked(item: I) {}
    fun onUserIconClicked(user: TweetUserItem) {}
}

interface TweetListItemViewModel : ListItemClickListener<TweetListItem> {
    val selectedItemId: LiveData<SelectedItemId?>
    fun onQuoteItemClicked(item: TweetListItem)
}
