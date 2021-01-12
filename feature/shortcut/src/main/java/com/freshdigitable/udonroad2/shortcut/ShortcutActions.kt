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

import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction

interface ShortcutActions {
    val showTweetDetail: AppAction<SelectedItemShortcut.TweetDetail>
    val favTweet: AppAction<SelectedItemShortcut.Like>
    val retweet: AppAction<SelectedItemShortcut.Retweet>
    val showConversation: AppAction<SelectedItemShortcut.Conversation>

    companion object {
        fun create(dispatcher: EventDispatcher): ShortcutActions = object : ShortcutActions {
            override val showTweetDetail: AppAction<SelectedItemShortcut.TweetDetail> =
                dispatcher.toAction()
            override val favTweet: AppAction<SelectedItemShortcut.Like> =
                dispatcher.toAction()
            override val retweet: AppAction<SelectedItemShortcut.Retweet> =
                dispatcher.toAction()
            override val showConversation: AppAction<SelectedItemShortcut.Conversation> =
                dispatcher.toAction()
        }
    }
}
