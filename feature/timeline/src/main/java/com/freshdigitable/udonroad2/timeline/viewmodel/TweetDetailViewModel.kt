package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.subscribeToUpdate
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.tweet.Tweet
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import com.freshdigitable.udonroad2.timeline.LaunchMediaViewerAction
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.broadcastIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transformLatest
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TweetDetailViewModel(
    private val eventDispatcher: EventDispatcher,
    private val viewStates: TweetDetailViewStates,
    coroutineContext: CoroutineContext? = null
) : TweetListItemClickListener, ViewModel() {
    private val coroutineContext: CoroutineContext =
        coroutineContext ?: viewModelScope.coroutineContext

    val tweetItem: LiveData<TweetListItem?> = viewStates.tweetItem
        .openSubscription()
        .receiveAsFlow()
        .asLiveData(this.coroutineContext)
    val menuItemStates: LiveData<TweetDetailViewStates.MenuItemState> = viewStates.menuItemState
        .asLiveData(this.coroutineContext)

    fun onOriginalUserClicked() {
        val user = tweetItem.value?.originalUser ?: return
        eventDispatcher.postEvent(
            TimelineEvent.RetweetUserClicked(user)
        )
    }

    fun onBodyUserClicked() {
        val user = tweetItem.value?.body?.user ?: return
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override fun onMediaItemClicked(
        originalId: TweetId,
        quotedId: TweetId?,
        item: Tweet,
        index: Int
    ) {
        eventDispatcher.postEvent(TimelineEvent.MediaItemClicked(item.id, index))
    }

    fun onMenuItemClicked(@IdRes itemId: Int) {
        Timber.tag("TweetDetailViewModel").d("onMenuItemClicked: $itemId")
        val tweetItem = checkNotNull(tweetItem.value)
        val tweetId = tweetItem.originalId
        val event: AppEvent = when (itemId) {
            R.id.detail_main_rt -> {
                if (!tweetItem.body.isRetweeted) {
                    SelectedItemShortcut.Retweet(tweetId)
                } else {
                    TODO()
                }
            }
            R.id.detail_main_fav -> {
                if (!tweetItem.body.isFavorited) {
                    SelectedItemShortcut.Like(tweetId)
                } else {
                    TODO()
                }
            }
            R.id.detail_main_reply -> SelectedItemShortcut.Reply(tweetId)
            R.id.detail_main_quote -> SelectedItemShortcut.Quote(tweetId)
            else -> return
        }
        eventDispatcher.postEvent(event)
    }

    override fun onCleared() {
        super.onCleared()
        viewStates.clear()
    }
}

class TweetDetailActions @Inject constructor(
    eventDispatcher: EventDispatcher
) : UserIconClickedAction by UserIconClickedAction.create(eventDispatcher),
    LaunchMediaViewerAction by LaunchMediaViewerAction.create(eventDispatcher) {
    val launchOriginalTweetUserInfo: AppAction<TimelineEvent.RetweetUserClicked> =
        eventDispatcher.toAction()
}

class TweetDetailViewStates @Inject constructor(
    tweetId: TweetId,
    actions: TweetDetailActions,
    repository: TweetRepository,
    oAuthTokenRepository: OAuthTokenRepository,
    activityEventDelegate: ActivityEventDelegate,
    executor: AppExecutor
) {
    internal val tweetItem = repository.getTweetItemSource(tweetId)
        .transformLatest {
            when {
                it != null -> emit(it)
                else -> {
                    val item = repository.findTweetListItem(tweetId)
                    item?.let { i -> emit(i) }
                }
            }
        }
        .catch {
            when (it) {
                is IOException -> {
                    // TODO
                }
                else -> throw it
            }
        }
        .broadcastIn(scope = executor)
    internal val menuItemState: Flow<MenuItemState> = tweetItem.openSubscription()
        .receiveAsFlow()
        .scan(MenuItemState()) { _, tweet ->
            MenuItemState(
                isMainGroupEnabled = true,
                isRetweetChecked = tweet.body.isRetweeted,
                isFavChecked = tweet.body.isFavorited,
                isDeleteVisible = oAuthTokenRepository.getCurrentUserId() == tweet.originalUser.id
            )
        }.distinctUntilChanged()

    data class MenuItemState(
        val isMainGroupEnabled: Boolean = false,
        val isRetweetChecked: Boolean = false,
        val isFavChecked: Boolean = false,
        val isDeleteVisible: Boolean = false
    )

    private val compositeDisposable = CompositeDisposable(
        actions.launchUserInfo.subscribeToUpdate(activityEventDelegate) {
            dispatchNavHostNavigate(TimelineEvent.Navigate.UserInfo(it.user))
        },
        actions.launchOriginalTweetUserInfo.subscribeToUpdate(activityEventDelegate) {
            dispatchNavHostNavigate(TimelineEvent.Navigate.UserInfo(it.user))
        },
        actions.launchMediaViewer.subscribeToUpdate(activityEventDelegate) {
            dispatchNavHostNavigate(TimelineEvent.Navigate.MediaViewer(it))
        },
    )

    fun clear() {
        compositeDisposable.clear()
    }
}
