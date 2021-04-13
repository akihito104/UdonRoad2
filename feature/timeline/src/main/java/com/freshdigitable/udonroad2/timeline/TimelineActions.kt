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
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.app.navigation.toListener
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.shortcut.ShortcutActions
import com.freshdigitable.udonroad2.timeline.TimelineEvent.TweetItemSelection

internal class TimelineActions(
    private val owner: ListOwner<*>,
    private val dispatcher: EventDispatcher,
    listItemLoadableActions: ListItemLoadableAction,
    mediaViewerAction: LaunchMediaViewerAction,
) : ListItemLoadableAction by listItemLoadableActions,
    ShortcutActions by ShortcutActions.create(dispatcher),
    TweetMediaAction by mediaViewerAction,
    TweetListItemEventListener {

    val toggleItem = dispatcher.toActionFlow<TweetItemSelection.Toggle> { it.owner == owner }
    override val selectBodyItem = dispatcher.toListener { item: TweetListItem ->
        TweetItemSelection.Toggle(SelectedItemId(owner, item.originalId))
    }
    override val toggleQuoteItem = dispatcher.toListener { item: TweetListItem ->
        TweetItemSelection.Toggle(SelectedItemId(owner, item.originalId, item.quoted?.id))
    }

    val selectItem = dispatcher.toAction { itemId: SelectedItemId ->
        TweetItemSelection.Selected(itemId)
    }

    override fun onMediaItemClicked(
        originalId: TweetId,
        quotedId: TweetId?,
        id: TweetId,
        index: Int,
    ) {
        selectItem.dispatch(SelectedItemId(owner, originalId, quotedId))
        onMediaItemClicked(id, index)
    }

    val unselectItem = dispatcher.toActionFlow<TweetItemSelection.Unselected> { it.owner == owner }
}
