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

import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import kotlinx.coroutines.flow.Flow

interface ShortcutActions {
    val showTweetDetail: Flow<SelectedItemShortcut.TweetDetail>
    val favTweet: Flow<SelectedItemShortcut.Like>
    val retweet: Flow<SelectedItemShortcut.Retweet>
    val showConversation: Flow<SelectedItemShortcut.Conversation>

    companion object {
        fun create(dispatcher: EventDispatcher): ShortcutActions = TweetShortcutActions(dispatcher)
    }
}

private class TweetShortcutActions(
    dispatcher: EventDispatcher,
) : ShortcutActions {
    override val showTweetDetail: Flow<SelectedItemShortcut.TweetDetail> =
        dispatcher.toActionFlow()
    override val favTweet: Flow<SelectedItemShortcut.Like> =
        dispatcher.toActionFlow()
    override val retweet: Flow<SelectedItemShortcut.Retweet> =
        dispatcher.toActionFlow()
    override val showConversation: Flow<SelectedItemShortcut.Conversation> =
        dispatcher.toActionFlow()
}
