package com.freshdigitable.udonroad2.user

import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.shortcut.postSelectedItemShortcutEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.user.UserActivityEvent.Relationships
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.EnumSet

class UserViewModel(
    private val tweetUserItem: TweetUserItem,
    private val eventDispatcher: EventDispatcher,
    private val viewState: UserActivityViewStates,
) : ViewModel() {
    val user: LiveData<UserEntity?> = viewState.user
    val relationship: LiveData<Relationship?> = viewState.relationship
    val titleAlpha: LiveData<Float> = viewState.titleAlpha
    val fabVisible: LiveData<Boolean> = viewState.fabVisible
    val relationshipMenuItems: LiveData<Set<RelationshipMenu>> = viewState.relationshipMenuItems
    val pages: Flow<Map<UserPage, ListOwner<*>>> = viewState.pages

    fun setAppBarScrollRate(rate: Float) {
        eventDispatcher.postEvent(UserActivityEvent.AppbarScrolled(rate))
    }

    fun setCurrentPage(index: Int) {
        eventDispatcher.postEvent(UserActivityEvent.PageChanged(UserPage.values()[index]))
    }

    fun onFabMenuSelected(item: MenuItem) {
        Timber.tag("UserViewModel").d("onFabSelected: $item")
        val selected =
            requireNotNull(viewState.selectedItemId.value) { "selectedItem should not be null." }
        eventDispatcher.postSelectedItemShortcutEvent(item, selected)
    }

    fun onBackPressed() {
        val selectedItem = viewState.selectedItemId.value
        val event = if (selectedItem != null) {
            TimelineEvent.TweetItemSelection.Unselected(selectedItem.owner)
        } else {
            CommonEvent.Back
        }
        eventDispatcher.postEvent(event)
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return eventDispatcher.postRelationshipEvent(tweetUserItem.id, item)
    }

    override fun onCleared() {
        super.onCleared()
        viewState.clear()
    }
}

private fun EventDispatcher.postRelationshipEvent(userId: UserId, item: MenuItem): Boolean {
    val menuItem = RelationshipMenu.findById(item.itemId) ?: return false
    postEvent(menuItem.event(userId))
    return true
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
