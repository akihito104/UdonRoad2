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

import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.shortcut.ShortcutActions
import com.freshdigitable.udonroad2.timeline.TimelineEvent.Init
import com.freshdigitable.udonroad2.timeline.TimelineEvent.TweetItemSelection
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

internal class TimelineActions(
    private val owner: ListOwner<*>,
    private val dispatcher: EventDispatcher,
    listItemLoadableActions: ListItemLoadableAction,
    mediaViewerAction: LaunchMediaViewerAction,
) : ListItemLoadableAction by listItemLoadableActions,
    ShortcutActions by ShortcutActions.create(dispatcher),
    TweetMediaAction by mediaViewerAction,
    TweetListItemEventListener {

    val showTimeline: Flow<Init> = dispatcher.toActionFlow()
    val selectItem = dispatcher.toActionFlow<TweetItemSelection.Selected> { it.owner == owner }
    val toggleItem = dispatcher.toActionFlow<TweetItemSelection.Toggle> { it.owner == owner }
    val unselectItem = dispatcher.toActionFlow<TweetItemSelection.Unselected> { it.owner == owner }

    override fun onBodyItemClicked(item: TweetListItem) {
        Timber.tag("TimelineViewModel").d("onBodyItemClicked: ${item.body.id}")
        updateSelectedItem(SelectedItemId(owner, item.originalId))
    }

    override fun onQuoteItemClicked(item: TweetListItem) {
        Timber.tag("TimelineViewModel").d("onQuoteItemClicked: ${item.quoted?.id}")
        updateSelectedItem(SelectedItemId(owner, item.originalId, item.quoted?.id))
    }

    private fun updateSelectedItem(selected: SelectedItemId) {
        dispatcher.postEvent(TweetItemSelection.Toggle(selected))
    }

    override fun onMediaItemClicked(
        originalId: TweetId,
        quotedId: TweetId?,
        item: TweetElement,
        index: Int
    ) {
        val selected = SelectedItemId(owner, originalId, quotedId)
        dispatcher.postEvent(TimelineEvent.MediaItemClicked(item.id, index, selected))
    }

    override fun onMediaItemClicked(originalId: TweetId, item: TweetElement, index: Int) {
        onMediaItemClicked(originalId, null, item, index)
    }
}
