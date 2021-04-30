/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad2.shortcut

import android.view.MenuItem
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import javax.inject.Inject

class ShortcutActions @Inject constructor(
    private val dispatcher: EventDispatcher,
) : ShortcutEventListener {
    internal val showTweetDetail = dispatcher.toActionFlow<SelectedItemShortcut.TweetDetail>()
    internal val favTweet = dispatcher.toActionFlow<SelectedItemShortcut.Like>()
    internal val retweet = dispatcher.toActionFlow<SelectedItemShortcut.Retweet>()
    internal val showConversation = dispatcher.toActionFlow<SelectedItemShortcut.Conversation>()
    internal val unlikeTweet = dispatcher.toActionFlow<SelectedItemShortcut.Unlike>()
    internal val unretweetTweet = dispatcher.toActionFlow<SelectedItemShortcut.Unretweet>()
    internal val deleteTweet = dispatcher.toActionFlow<SelectedItemShortcut.DeleteTweet>()

    override fun onShortcutMenuSelected(item: MenuItem, id: TweetId) {
        dispatcher.postSelectedItemShortcutEvent(item, id)
    }
}

fun EventDispatcher.postSelectedItemShortcutEvent(
    menuItem: MenuItem,
    selectedItemId: SelectedItemId,
) {
    val tweetId = selectedItemId.quoteId ?: selectedItemId.originalId
    postSelectedItemShortcutEvent(menuItem, tweetId)
}

fun EventDispatcher.postSelectedItemShortcutEvent(
    menuItem: MenuItem,
    tweetId: TweetId,
) {
    when (menuItem.itemId) {
        R.id.iffabMenu_main_detail -> postEvent(SelectedItemShortcut.TweetDetail(tweetId))
        R.id.iffabMenu_main_fav -> postEvent(SelectedItemShortcut.Like(tweetId))
        R.id.iffabMenu_main_rt -> postEvent(SelectedItemShortcut.Retweet(tweetId))
        R.id.iffabMenu_main_favRt -> postEvents(
            SelectedItemShortcut.Like(tweetId),
            SelectedItemShortcut.Retweet(tweetId)
        )
        R.id.iffabMenu_main_reply -> postEvent(SelectedItemShortcut.Reply(tweetId))
        R.id.iffabMenu_main_quote -> postEvent(SelectedItemShortcut.Quote(tweetId))
        R.id.iffabMenu_main_conv -> postEvent(SelectedItemShortcut.Conversation(tweetId))
        R.id.detail_main_rt -> {
            if (menuItem.isChecked) {
                postEvent(SelectedItemShortcut.Unretweet(tweetId))
            } else {
                postEvent(SelectedItemShortcut.Retweet(tweetId))
            }
        }
        R.id.detail_main_fav -> {
            if (menuItem.isChecked) {
                postEvent(SelectedItemShortcut.Unlike(tweetId))
            } else {
                postEvent(SelectedItemShortcut.Like(tweetId))
            }
        }
        R.id.detail_main_reply -> postEvent(SelectedItemShortcut.Reply(tweetId))
        R.id.detail_main_quote -> postEvent(SelectedItemShortcut.Quote(tweetId))
        R.id.detail_more_delete -> postEvent(SelectedItemShortcut.DeleteTweet(tweetId))
        R.id.detail_main_conv -> postEvent(SelectedItemShortcut.Conversation(tweetId))
        else -> throw IllegalStateException("selected unregistered menu item: $menuItem")
    }
}
