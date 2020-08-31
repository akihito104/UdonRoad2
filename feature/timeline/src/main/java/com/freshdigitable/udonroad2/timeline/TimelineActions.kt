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

package com.freshdigitable.udonroad2.timeline

import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.filterByType
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.timeline.TimelineEvent.Init
import com.freshdigitable.udonroad2.timeline.TimelineEvent.MediaItemClicked
import com.freshdigitable.udonroad2.timeline.TimelineEvent.RetweetUserClicked
import com.freshdigitable.udonroad2.timeline.TimelineEvent.SelectedItemShortcut
import com.freshdigitable.udonroad2.timeline.TimelineEvent.TweetItemSelection
import com.freshdigitable.udonroad2.timeline.TimelineEvent.UserIconClicked
import javax.inject.Inject

class TimelineActions @Inject constructor(
    dispatcher: EventDispatcher,
) {
    val showTimeline: AppAction<Init> = dispatcher.toAction()
    val showTweetDetail: AppAction<SelectedItemShortcut.TweetDetail> = dispatcher.toAction()

    val launchUserInfo: AppAction<TweetingUser> = dispatcher.toAction {
        AppAction.merge(
            filterByType<UserIconClicked>().map { it.user },
            filterByType<RetweetUserClicked>().map { it.user }
        )
    }
    val launchMediaViewer: AppAction<MediaItemClicked> = dispatcher.toAction()

    val selectItem: AppAction<TweetItemSelection.Selected> = dispatcher.toAction {
        AppAction.merge(
            filterByType<TweetItemSelection.Selected>(),
            launchMediaViewer
                .filter { it.selectedItemId != null }
                .map { TweetItemSelection.Selected(requireNotNull(it.selectedItemId)) }
        )
    }
    val toggleItem: AppAction<TweetItemSelection.Toggle> = dispatcher.toAction()
    val unselectItem: AppAction<TweetItemSelection.Unselected> = dispatcher.toAction()

    val favTweet: AppAction<SelectedItemShortcut.Like> = dispatcher.toAction()
    val retweet: AppAction<SelectedItemShortcut.Retweet> = dispatcher.toAction()
}