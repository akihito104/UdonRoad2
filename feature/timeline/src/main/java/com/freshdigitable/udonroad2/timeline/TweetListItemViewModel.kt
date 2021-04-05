package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetListItem

interface ListItemClickListener<I> {
    fun onBodyItemClicked(item: I)
}

interface TweetListItemEventListener : ListItemClickListener<TweetListItem> {
    fun onQuoteItemClicked(item: TweetListItem)
    fun onMediaItemClicked(originalId: TweetId, item: TweetElement, index: Int) {
        onMediaItemClicked(originalId, null, item, index)
    }

    fun onMediaItemClicked(originalId: TweetId, quotedId: TweetId?, item: TweetElement, index: Int)
}

interface TweetListItemViewModel : TweetListItemEventListener {
    val selectedItemId: LiveData<SelectedItemId?>
}
