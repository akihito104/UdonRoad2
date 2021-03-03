package com.freshdigitable.udonroad2.user

import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.shortcut.postSelectedItemShortcutEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.user.UserActivityEvent.Relationships
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import java.util.EnumSet

class UserViewModel(
    private val eventDispatcher: EventDispatcher,
    private val viewState: UserViewModelSource,
) : UserViewEventListener by viewState,
    ViewModel() {
    val state: LiveData<UserViewState> = viewState.state.asLiveData(viewModelScope.coroutineContext)
    val relationshipMenuItems: LiveData<Set<RelationshipMenu>> =
        state.map { it.relationshipMenuItems }.distinctUntilChanged()
    internal val pages: Flow<Map<UserPage, ListOwner<*>>> = state.map { it.pages }
        .asFlow()
        .distinctUntilChanged()
    internal val feedbackMessage: Flow<FeedbackMessage> = viewState.feedbackMessage

    fun onFabMenuSelected(item: MenuItem) {
        Timber.tag("UserViewModel").d("onFabSelected: $item")
        val selected =
            requireNotNull(state.value?.selectedItemId) { "selectedItem should not be null." }
        eventDispatcher.postSelectedItemShortcutEvent(item, selected)
    }

    override fun onBackPressed(): Boolean {
        val selectedItem = state.value?.selectedItemId
        val event = if (selectedItem != null) {
            TimelineEvent.TweetItemSelection.Unselected(selectedItem.owner)
        } else {
            return false
        }
        eventDispatcher.postEvent(event)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        viewState.clear()
    }
}

interface UserViewEventListener {
    fun onAppBarScrolled(rate: Float)
    fun onCurrentPageChanged(index: Int)
    fun onOptionsItemSelected(item: MenuItem): Boolean
    fun onBackPressed(): Boolean
}

interface UserViewState {
    val user: UserEntity?
    val relationship: Relationship?
    val relationshipMenuItems: Set<RelationshipMenu>
    val titleAlpha: Float
    val pages: Map<UserPage, ListOwner<*>>
    val isShortcutVisible: Boolean
    val selectedItemId: SelectedItemId?
}

@Keep
enum class RelationshipMenu(
    @IdRes val id: Int,
    val event: (UserId) -> Relationships
) {
    FOLLOW(R.id.action_follow, { u -> Relationships.Following(true, u) }),
    UNFOLLOW(R.id.action_unfollow, { u -> Relationships.Following(false, u) }),
    BLOCK(R.id.action_block, { u -> Relationships.Blocking(true, u) }),
    UNBLOCK(R.id.action_unblock, { u -> Relationships.Blocking(false, u) }),
    MUTE(R.id.action_mute, { u -> Relationships.Muting(true, u) }),
    UNMUTE(R.id.action_unmute, { u -> Relationships.Muting(false, u) }),
    RETWEET_BLOCKED(R.id.action_block_retweet, { u -> Relationships.WantsRetweet(false, u) }),
    RETWEET_WANTED(R.id.action_unblock_retweet, { u -> Relationships.WantsRetweet(true, u) }),
    REPORT_SPAM(R.id.action_r4s, { u -> Relationships.ReportSpam(u) }),
    ;

    companion object {
        fun findById(@IdRes id: Int): RelationshipMenu? = values().firstOrNull { it.id == id }

        fun availableItems(relationship: Relationship?): EnumSet<RelationshipMenu> {
            if (relationship == null) {
                return EnumSet.of(REPORT_SPAM)
            }
            return EnumSet.of(
                if (relationship.following) UNFOLLOW else FOLLOW,
                if (relationship.blocking) UNBLOCK else BLOCK,
                if (relationship.muting) UNMUTE else MUTE,
                REPORT_SPAM,
            ).apply {
                if (relationship.following) {
                    if (relationship.wantRetweets) add(RETWEET_BLOCKED) else add(RETWEET_WANTED)
                }
            }
        }
    }
}
