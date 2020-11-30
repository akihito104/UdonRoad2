package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.subscribeToUpdate
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.tweet.Tweet
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.timeline.LaunchMediaViewerAction
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.transformLatest
import java.io.IOException
import javax.inject.Inject

class TweetDetailViewModel(
    private val eventDispatcher: EventDispatcher,
    private val viewStates: TweetDetailViewStates,
) : TweetListItemClickListener, ViewModel() {

    val tweetItem: LiveData<TweetListItem?> = viewStates.tweetItem

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
    activityEventDelegate: ActivityEventDelegate,
    executor: AppExecutor,
) {
    private val _tweetItem: Flow<TweetListItem?> = repository.getTweetItemSource(tweetId)
        .transformLatest {
            if (it != null) {
                emit(it)
            } else {
                val item = repository.findTweetListItem(tweetId)
                if (item != null) {
                    emit(item)
                }
            }
        }
        .catch {
            if (it is IOException) {
                // TODO
            } else {
                throw it
            }
        }
    val tweetItem = _tweetItem.asLiveData(executor.dispatcher.mainContext)

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
