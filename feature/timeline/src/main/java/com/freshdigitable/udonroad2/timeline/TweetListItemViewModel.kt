package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem

interface ListItemClickListener<I> {
    fun onBodyItemClicked(item: I)
}

interface TweetListItemEventListener : ListItemClickListener<TweetListItem> {
    fun onQuoteItemClicked(item: TweetListItem)
}

interface TweetListItemViewModel : TweetListItemEventListener {
    val selectedItemId: LiveData<SelectedItemId?>
}
