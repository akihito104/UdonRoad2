package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener1
import com.freshdigitable.udonroad2.model.tweet.TweetListItem

interface ListItemClickListener<I> {
    val selectBodyItem: AppEventListener1<I>
}

interface TweetListItemEventListener : ListItemClickListener<TweetListItem> {
    val toggleQuoteItem: AppEventListener1<TweetListItem>
    fun onMediaItemClicked(originalId: TweetId, id: TweetId, index: Int) {
        onMediaItemClicked(originalId, null, id, index)
    }

    fun onMediaItemClicked(originalId: TweetId, quotedId: TweetId?, id: TweetId, index: Int)
}

interface TweetListItemViewModel : TweetListItemEventListener {
    val selectedItemId: LiveData<SelectedItemId?>
}
