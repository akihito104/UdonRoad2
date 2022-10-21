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

import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectDelegate
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectStream
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentEffectDelegate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

internal class TimelineViewModelSource(
    owner: ListOwner<QueryType.Tweet>,
    actions: TimelineActions,
    selectedItemRepository: SelectedItemRepository,
    private val baseViewModelSource: ListItemLoadableViewModelSource,
    mediaViewModelSource: TweetMediaViewModelSource,
) : ListItemLoadableViewModelSource by baseViewModelSource,
    TweetMediaViewModelSource by mediaViewModelSource,
    ActivityEffectStream,
    TweetListItemEventListener by actions {

    override val state: Flow<TimelineState> = stateSourceBuilder(
        init = TimelineState(
            selectedItemId = selectedItemRepository.find(owner)
        ),
        baseViewModelSource.state.onEvent { s, base -> s.copy(baseState = base) },
        actions.selectItem.onEvent { s, e ->
            selectedItemRepository.put(e.selectedItemId)
            s
        },
        actions.unselectItem.onEvent { s, _ ->
            selectedItemRepository.remove(owner)
            s
        },
        actions.toggleItem.onEvent { s, e ->
            when (s.selectedItemId) {
                e.item -> selectedItemRepository.remove(owner)
                else -> selectedItemRepository.put(e.item)
            }
            s
        },
        actions.heading.onEvent { s, _ ->
            selectedItemRepository.remove(owner)
            s
        },
        selectedItemRepository.getSource(owner).onEvent { s, item ->
            s.copy(selectedItemId = item)
        }
    )

    override val effect: Flow<AppEffect> = merge(
        mediaViewModelSource.effect,
        baseViewModelSource.effect,
    )

    override fun clear() {
        super.clear()
        baseViewModelSource.clear()
    }
}

data class TimelineState(
    val baseState: ListItemLoadableViewModel.State? = null,
    override val selectedItemId: SelectedItemId? = null,
) : ListItemLoadableViewModel.State, TweetListItemViewModel.State {
    override val isHeadingEnabled: Boolean
        get() = baseState?.isHeadingEnabled == true || selectedItemId != null
    override val isPrepending: Boolean
        get() = baseState?.isPrepending ?: false
    override val isHeadingVisible: Boolean
        get() = baseState?.isHeadingVisible ?: false
}

class TimelineEffectDelegate @Inject constructor(
    activityEffectDelegate: ActivityEffectDelegate,
) : ListItemFragmentEffectDelegate,
    ActivityEffectDelegate by activityEffectDelegate
