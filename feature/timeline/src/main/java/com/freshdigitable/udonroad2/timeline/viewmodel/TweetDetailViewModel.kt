package com.freshdigitable.udonroad2.timeline.viewmodel

import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.AppTwitterException.ErrorType
import com.freshdigitable.udonroad2.model.app.isTwitterExceptionOf
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import com.freshdigitable.udonroad2.shortcut.ShortcutActions
import com.freshdigitable.udonroad2.shortcut.ShortcutViewStates
import com.freshdigitable.udonroad2.shortcut.TweetContextMenuEvent
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetMediaEventListener
import com.freshdigitable.udonroad2.timeline.TweetMediaItemViewModel
import com.freshdigitable.udonroad2.timeline.TweetMediaViewModelSource
import com.freshdigitable.udonroad2.timeline.UserIconClickListener
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import com.freshdigitable.udonroad2.timeline.getTimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.rx2.asFlow
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TweetDetailViewModel(
    private val viewStates: TweetDetailViewStates,
    userIconViewModelSource: UserIconViewModelSource,
    mediaViewModelSource: TweetMediaViewModelSource,
    coroutineContext: CoroutineContext? = null
) : TweetDetailEventListener by viewStates,
    UserIconClickListener by userIconViewModelSource,
    TweetMediaItemViewModel,
    TweetMediaEventListener by mediaViewModelSource,
    ActivityEventStream by viewStates,
    ViewModel() {
    private val coroutineContext: CoroutineContext =
        coroutineContext ?: viewModelScope.coroutineContext

    val state: LiveData<State> = viewStates.viewModelState.asLiveData(this.coroutineContext)
    override val mediaState: LiveData<TweetMediaItemViewModel.State> = mediaViewModelSource.state
        .asLiveData(this.coroutineContext)
    override val navigationEvent: Flow<NavigationEvent> = merge(
        viewStates.navigationEvent,
        userIconViewModelSource.navEvent,
        mediaViewModelSource.navigationEvent
    )

    interface State {
        val tweetItem: TweetListItem?
        val menuItemState: TweetDetailViewStates.MenuItemState
    }
}

sealed class DetailMenuEvent : TweetContextMenuEvent {
    data class Unlike(override val tweetId: TweetId) : DetailMenuEvent()
    data class Unretweet(override val tweetId: TweetId) : DetailMenuEvent()
    data class DeleteTweet(override val tweetId: TweetId) : DetailMenuEvent()
}

interface TweetDetailEventListener {
    fun onOriginalUserClicked(user: TweetUserItem)
    fun onBodyUserClicked(user: TweetUserItem)
    fun onMenuItemClicked(item: MenuItem, tweetItem: TweetListItem)
}

class TweetDetailActions @Inject constructor(
    private val eventDispatcher: EventDispatcher,
) : TweetDetailEventListener,
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
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override fun onMenuItemClicked(item: MenuItem, tweetItem: TweetListItem) {
        val tweetId = tweetItem.originalId
        val event: AppEvent = when (item.itemId) {
            R.id.detail_main_rt -> {
                if (tweetItem.body.isRetweeted) {
                    DetailMenuEvent.Unretweet(tweetId)
                } else {
                    SelectedItemShortcut.Retweet(tweetId)
                }
            }
            R.id.detail_main_fav -> {
                if (tweetItem.body.isFavorited) {
                    DetailMenuEvent.Unlike(tweetId)
                } else {
                    SelectedItemShortcut.Like(tweetId)
                }
            }
            R.id.detail_main_reply -> SelectedItemShortcut.Reply(tweetId)
            R.id.detail_main_quote -> SelectedItemShortcut.Quote(tweetId)
            R.id.detail_more_delete -> DetailMenuEvent.DeleteTweet(
                tweetItem.body.retweetIdByCurrentUser ?: tweetId
            )
            R.id.detail_main_conv -> SelectedItemShortcut.Conversation(tweetId)
            else -> throw NotImplementedError("detail menu: $item is not implemented yet...")
        }
        eventDispatcher.postEvent(event)
    }
}

class TweetDetailViewStates @Inject constructor(
    tweetId: TweetId,
    actions: TweetDetailActions,
    repository: TweetRepository,
    appSettingRepository: AppSettingRepository,
    listOwnerGenerator: ListOwnerGenerator,
) : TweetDetailEventListener by actions,
    ShortcutViewStates by ShortcutViewStates.create(actions, repository),
    ActivityEventStream {

    internal val viewModelState: Flow<TweetDetailViewModel.State> = stateSourceBuilder(
        init = ViewModelState(currentUserId = appSettingRepository.currentUserId),
        repository.getDetailTweetItemSource(tweetId).onEvent { s, item ->
            when {
                item != null -> s.copy(tweetItem = item)
                s.isTweetItemDeleted -> s
                else -> {
                    repository.runCatching { findDetailTweetItem(tweetId) }.fold(
                        onSuccess = { tweetItem ->
                            s.copy(tweetItem = tweetItem, isTweetItemDeleted = tweetItem == null)
                        },
                        onFailure = {
                            when {
                                it.isTwitterExceptionOf(ErrorType.TWEET_NOT_FOUND) -> {
                                    s.copy(tweetItem = null, isTweetItemDeleted = false)
                                }
                                it is IOException -> s
                                else -> throw it
                            }
                        }
                    )
                }
            }
        },
        appSettingRepository.currentUserIdSource.onEvent { s, id -> s.copy(currentUserId = id) },
        actions.unlikeTweet.asFlow().onEvent { s, e ->
            repository.updateLike(e.tweetId, false)
            s
        },
        actions.unretweetTweet.asFlow().onEvent { s, e ->
            repository.updateRetweet(e.tweetId, false)
            s
        },
        actions.deleteTweet.asFlow().onEvent { s, e ->
            repository.deleteTweet(e.tweetId)
            s.copy(isTweetItemDeleted = true)
        },
    )

    override val navigationEvent: Flow<NavigationEvent> = merge(
        actions.launchOriginalTweetUserInfo.mapLatest { TimelineEvent.Navigate.UserInfo(it.user) },
        actions.showConversation.mapLatest {
            listOwnerGenerator.getTimelineEvent(
                QueryType.TweetQueryType.Conversation(it.tweetId),
                NavigationEvent.Type.NAVIGATE
            )
        }
    )
    override val feedbackMessage: Flow<FeedbackMessage> = updateTweet

    private data class ViewModelState(
        override val tweetItem: TweetListItem? = null,
        val isTweetItemDeleted: Boolean = false,
        val currentUserId: UserId? = null,
    ) : TweetDetailViewModel.State {
        override val menuItemState: MenuItemState
            get() = when (tweetItem) {
                null -> MenuItemState()
                else -> MenuItemState(
                    isMainGroupEnabled = true,
                    isRetweetChecked = tweetItem.body.isRetweeted,
                    isFavChecked = tweetItem.body.isFavorited,
                    isDeleteVisible = currentUserId?.let { it == tweetItem.originalUser.id }
                        ?: false
                )
            }
    }

    data class MenuItemState(
        val isMainGroupEnabled: Boolean = false,
        val isRetweetChecked: Boolean = false,
        val isFavChecked: Boolean = false,
        val isDeleteVisible: Boolean = false
    )
}
