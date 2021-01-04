package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.mainContext
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.suspendMap
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import com.freshdigitable.udonroad2.shortcut.TweetContextMenuEvent
import com.freshdigitable.udonroad2.timeline.LaunchMediaViewerAction
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.rx2.asFlow
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
        .asLiveData(this.coroutineContext)
    val menuItemStates: LiveData<TweetDetailViewStates.MenuItemState> = viewStates.menuItemState
        .asLiveData(this.coroutineContext)
    internal val navigationEvent: Flow<NavigationEvent> = viewStates.navigateEvent

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
        item: TweetElement,
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
                    DetailMenuEvent.Unretweet(tweetId)
                }
            }
            R.id.detail_main_fav -> {
                if (!tweetItem.body.isFavorited) {
                    SelectedItemShortcut.Like(tweetId)
                } else {
                    DetailMenuEvent.Unlike(tweetId)
                }
            }
            R.id.detail_main_reply -> SelectedItemShortcut.Reply(tweetId)
            R.id.detail_main_quote -> SelectedItemShortcut.Quote(tweetId)
            R.id.detail_more_delete -> DetailMenuEvent.DeleteTweet(tweetId)
            R.id.detail_main_conv -> return // todo
            else -> throw NotImplementedError("detail menu: $itemId is not implemented yet...")
        }
        eventDispatcher.postEvent(event)
    }

    override fun onCleared() {
        super.onCleared()
        viewStates.clear()
    }
}

sealed class DetailMenuEvent : TweetContextMenuEvent {
    data class Unlike(override val tweetId: TweetId) : DetailMenuEvent()
    data class Unretweet(override val tweetId: TweetId) : DetailMenuEvent()
    data class DeleteTweet(override val tweetId: TweetId) : DetailMenuEvent()
}

class TweetDetailActions @Inject constructor(
    eventDispatcher: EventDispatcher
) : UserIconClickedAction by UserIconClickedAction.create(eventDispatcher),
    LaunchMediaViewerAction by LaunchMediaViewerAction.create(eventDispatcher) {
    val launchOriginalTweetUserInfo: AppAction<TimelineEvent.RetweetUserClicked> =
        eventDispatcher.toAction()
    val unlikeTweet: AppAction<DetailMenuEvent.Unlike> = eventDispatcher.toAction()
    val unretweetTweet: AppAction<DetailMenuEvent.Unretweet> = eventDispatcher.toAction()
    val deleteTweet: AppAction<DetailMenuEvent.DeleteTweet> = eventDispatcher.toAction()
}

class TweetDetailViewStates @Inject constructor(
    tweetId: TweetId,
    actions: TweetDetailActions,
    repository: TweetRepository,
    oAuthTokenRepository: OAuthTokenRepository,
    executor: AppExecutor
) {
    private val coroutineScope = CoroutineScope(context = executor.mainContext)
    internal val tweetItem: StateFlow<TweetListItem?> = repository.getTweetItemSource(tweetId)
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
                is AppTwitterException -> {
                    if (it.errorType?.statusCode == 404 && it.errorType?.errorCode == 144) {
                        // TODO tweet resource is not found
                        coroutineScope.cancel("tweet resource is not found...")
                    }
                }
                is IOException -> {
                    // TODO
                }
                else -> throw it
            }
        }
        .stateIn(scope = coroutineScope, started = SharingStarted.Eagerly, null)
    internal val menuItemState: Flow<MenuItemState> = tweetItem.scan(MenuItemState()) { _, tweet ->
        when (tweet) {
            null -> MenuItemState()
            else -> MenuItemState(
                isMainGroupEnabled = true,
                isRetweetChecked = tweet.body.isRetweeted,
                isFavChecked = tweet.body.isFavorited,
                isDeleteVisible = oAuthTokenRepository.getCurrentUserId() == tweet.originalUser.id
            )
        }
    }.distinctUntilChanged()

    internal val navigateEvent: Flow<NavigationEvent> = merge(
        actions.launchUserInfo.asFlow().mapLatest { TimelineEvent.Navigate.UserInfo(it.user) },
        actions.launchOriginalTweetUserInfo.asFlow().mapLatest {
            TimelineEvent.Navigate.UserInfo(it.user)
        },
        actions.launchMediaViewer.asFlow().mapLatest { TimelineEvent.Navigate.MediaViewer(it) }
    )

    data class MenuItemState(
        val isMainGroupEnabled: Boolean = false,
        val isRetweetChecked: Boolean = false,
        val isFavChecked: Boolean = false,
        val isDeleteVisible: Boolean = false
    )

    private val compositeDisposable = CompositeDisposable(
        actions.unlikeTweet.suspendMap {
            repository.postUnlike(it.tweetId)
        }.subscribe(),
        actions.unretweetTweet.suspendMap {
            repository.postUnretweet(it.tweetId)
        }.subscribe(),
        actions.deleteTweet.suspendMap {
            repository.deleteTweet(it.tweetId)
        }.subscribe()
    )

    fun clear() {
        compositeDisposable.clear()
        coroutineScope.cancel()
    }
}
