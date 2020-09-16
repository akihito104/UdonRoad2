package com.freshdigitable.udonroad2.user

import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.postSelectedItemShortcutEvent
import com.freshdigitable.udonroad2.user.UserActivityEvent.Relationships
import timber.log.Timber

class UserViewModel(
    private val tweetingUser: TweetingUser,
    private val eventDispatcher: EventDispatcher,
    private val viewState: UserActivityViewStates,
) : ViewModel() {
    val user: LiveData<User?> = viewState.user
    val relationship: LiveData<Relationship?> = viewState.relationship
    val titleAlpha: LiveData<Float> = viewState.titleAlpha
    val fabVisible: LiveData<Boolean> = viewState.fabVisible

    fun getOwner(userPage: UserPage): ListOwner<*> = requireNotNull(viewState.pages[userPage])

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
        return eventDispatcher.postRelationshipEvent(tweetingUser.id, item)
    }
}

private fun EventDispatcher.postRelationshipEvent(userId: UserId, item: MenuItem): Boolean {
    val event = when (item.itemId) {
        R.id.action_follow -> Relationships.Following(true, userId)
        R.id.action_unfollow -> Relationships.Following(false, userId)
        R.id.action_block -> Relationships.Blocking(true, userId)
        R.id.action_unblock -> Relationships.Blocking(false, userId)
        R.id.action_mute -> Relationships.Muting(true, userId)
        R.id.action_unmute -> Relationships.Muting(false, userId)
        R.id.action_block_retweet -> Relationships.WantsRetweet(false, userId)
        R.id.action_unblock_retweet -> Relationships.WantsRetweet(true, userId)
        R.id.action_r4s -> Relationships.ReportSpam(userId)
        else -> return false
    }
    postEvent(event)
    return true
}
