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

package com.freshdigitable.udonroad2.shortcut_actions

import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.tweet.TweetId

sealed class SelectedItemShortcut : AppEvent {
    data class TweetDetail(override val tweetId: TweetId) : SelectedItemShortcut()
    data class Like(override val tweetId: TweetId) : SelectedItemShortcut()
    data class Retweet(override val tweetId: TweetId) : SelectedItemShortcut()

    abstract val tweetId: TweetId

    companion object
}
