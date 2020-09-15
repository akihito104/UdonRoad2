package com.freshdigitable.udonroad2.user

import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
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

    fun updateFollowingStatus(following: Boolean) {
        eventDispatcher.postEvent(Relationships.Following(following, tweetingUser.id))
    }

    fun updateBlockingStatus(blocking: Boolean) {
        eventDispatcher.postEvent(Relationships.Blocking(blocking, tweetingUser.id))
    }

    fun updateMutingStatus(muting: Boolean) {
        eventDispatcher.postEvent(Relationships.Muting(muting, tweetingUser.id))
    }

    fun updateWantRetweet(wantRetweet: Boolean) {
        eventDispatcher.postEvent(Relationships.WantsRetweet(wantRetweet, tweetingUser.id))
    }

    fun reportForSpam() {
        eventDispatcher.postEvent(Relationships.ReportSpam(tweetingUser.id))
    }
}
