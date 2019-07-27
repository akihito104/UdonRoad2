package com.freshdigitable.udonroad2.timeline

import androidx.databinding.ObservableField
import com.freshdigitable.udonroad2.model.Tweet
import com.freshdigitable.udonroad2.model.TweetListItem

interface TweetListItemClickListener {
    fun onBodyItemClicked(item: TweetListItem) {}

    fun onQuoteItemClicked(item: TweetListItem) {}

    fun onUserIconClicked(item: TweetListItem) {}

    fun onMediaItemClicked(originalId: Long, item: Tweet, index: Int)
}

interface TweetListEventListener {
    val selectedItemId: ObservableField<SelectedItemId?>
}

data class SelectedItemId(
    val originalId: Long,
    val quoteId: Long? = null
) {
    @JvmOverloads
    fun equalsTo(originalId: Long, quoteId: Long? = null): Boolean {
        return this.originalId == originalId && this.quoteId == quoteId
    }
}
