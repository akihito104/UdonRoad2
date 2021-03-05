package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.mainContext
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.suspendMap
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import com.freshdigitable.udonroad2.shortcut.ShortcutActions
import com.freshdigitable.udonroad2.shortcut.ShortcutViewStates
import com.freshdigitable.udonroad2.shortcut.TweetContextMenuEvent
import com.freshdigitable.udonroad2.timeline.LaunchUserInfoAction
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetMediaEventListener
import com.freshdigitable.udonroad2.timeline.TweetMediaItemViewModel
import com.freshdigitable.udonroad2.timeline.TweetMediaViewModelSource
import com.freshdigitable.udonroad2.timeline.UserIconClickListener
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import com.freshdigitable.udonroad2.timeline.getTimelineEvent
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
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TweetDetailViewModel(
    private val eventDispatcher: EventDispatcher,
    private val viewStates: TweetDetailViewStates,
    coroutineContext: CoroutineContext? = null
) : TweetDetailEventListener by viewStates,
    TweetMediaItemViewModel,
    TweetMediaEventListener by viewStates,
    ViewModel() {
    private val coroutineContext: CoroutineContext =
        coroutineContext ?: viewModelScope.coroutineContext

    val tweetItem: LiveData<TweetListItem?> = viewStates.tweetItem
        .asLiveData(this.coroutineContext)
    val menuItemStates: LiveData<TweetDetailViewStates.MenuItemState> = viewStates.menuItemState
        .asLiveData(this.coroutineContext)
    override val mediaState: LiveData<TweetMediaItemViewModel.State> =
        viewStates.state.asLiveData(this.coroutineContext)
    internal val navigationEvent: Flow<NavigationEvent> = viewStates.navigationEvent

    fun onOriginalUserClicked() {
        val user = tweetItem.value?.originalUser ?: return
        onOriginalUserClicked(user)
    }

    fun onBodyUserClicked() {
        val user = tweetItem.value?.body?.user ?: return
        onBodyUserClicked(user)
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
            R.id.detail_more_delete -> DetailMenuEvent.DeleteTweet(
                tweetItem.body.retweetIdByCurrentUser ?: tweetId
            )
            R.id.detail_main_conv -> SelectedItemShortcut.Conversation(tweetId)
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

interface TweetDetailEventListener : UserIconClickListener {
    fun onOriginalUserClicked(user: TweetUserItem)
    fun onBodyUserClicked(user: TweetUserItem)
}

class TweetDetailActions @Inject constructor(
    private val eventDispatcher: EventDispatcher,
    private val userIconClickedAction: UserIconClickedAction,
) : TweetDetailEventListener,
    LaunchUserInfoAction by userIconClickedAction,
    ShortcutActions by ShortcutActions.create(eventDispatcher) {
    val launchOriginalTweetUserInfo =
        eventDispatcher.toActionFlow<TimelineEvent.RetweetUserClicked>()
    val unlikeTweet = eventDispatcher.toAction<DetailMenuEvent.Unlike>()
    val unretweetTweet: AppAction<DetailMenuEvent.Unretweet> = eventDispatcher.toAction()
    val deleteTweet: AppAction<DetailMenuEvent.DeleteTweet> = eventDispatcher.toAction()

    override fun onOriginalUserClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.RetweetUserClicked(user))
    }

    override fun onBodyUserClicked(user: TweetUserItem) {
        onUserIconClicked(user)
    }

    override fun onUserIconClicked(user: TweetUserItem) {
        userIconClickedAction.onUserIconClicked(user)
    }
}

class TweetDetailViewStates @Inject constructor(
    tweetId: TweetId,
    actions: TweetDetailActions,
    repository: TweetRepository,
    appSettingRepository: AppSettingRepository,
    listOwnerGenerator: ListOwnerGenerator,
    executor: AppExecutor,
    mediaViewModelSource: TweetMediaViewModelSource,
    userIconViewModelSource: UserIconViewModelSource,
) : TweetDetailEventListener by actions,
    TweetMediaViewModelSource by mediaViewModelSource,
    ShortcutViewStates by ShortcutViewStates.create(actions, repository),
    ActivityEventStream by ActivityEventStream.EmptyStream {
    private val coroutineScope = CoroutineScope(context = executor.mainContext)

    internal val tweetItem: StateFlow<TweetListItem?> = repository.getDetailTweetItemSource(tweetId)
        .transformLatest {
            when {
                it != null -> emit(it)
                else -> {
                    val item = repository.findDetailTweetItem(tweetId)
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
                isDeleteVisible = appSettingRepository.currentUserId == tweet.originalUser.id
            )
        }
    }.distinctUntilChanged()

    override val navigationEvent: Flow<NavigationEvent> = merge(
        userIconViewModelSource.navEvent,
        actions.launchOriginalTweetUserInfo.mapLatest { TimelineEvent.Navigate.UserInfo(it.user) },
        mediaViewModelSource.navigationEvent,
        actions.showConversation.mapLatest {
            listOwnerGenerator.getTimelineEvent(
                QueryType.TweetQueryType.Conversation(it.tweetId),
                NavigationEvent.Type.NAVIGATE
            )
        }
    )
    override val feedbackMessage: Flow<FeedbackMessage> = updateTweet

    data class MenuItemState(
        val isMainGroupEnabled: Boolean = false,
        val isRetweetChecked: Boolean = false,
        val isFavChecked: Boolean = false,
        val isDeleteVisible: Boolean = false
    )

    private val compositeDisposable = CompositeDisposable(
        actions.unlikeTweet.suspendMap {
            repository.updateLike(it.tweetId, false)
        }.subscribe(),
        actions.unretweetTweet.suspendMap {
            repository.updateRetweet(it.tweetId, false)
        }.subscribe(),
        actions.deleteTweet.suspendMap {
            repository.deleteTweet(it.tweetId)
        }.subscribe(),
    )

    fun clear() {
        compositeDisposable.clear()
        coroutineScope.cancel()
    }
}
