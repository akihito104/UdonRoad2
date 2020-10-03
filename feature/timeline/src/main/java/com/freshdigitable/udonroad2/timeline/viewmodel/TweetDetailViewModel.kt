package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.di.ViewModelScope
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.tweet.Tweet
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.timeline.LaunchMediaViewerAction
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class TweetDetailViewModel(
    tweetId: TweetId,
    private val eventDispatcher: EventDispatcher,
    private val viewStates: TweetDetailViewStates,
    repository: TweetRepository,
) : TweetListItemClickListener, ViewModel() {

    val tweetItem: LiveData<TweetListItem?> = repository.getTweetItem(tweetId)

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
    actions: TweetDetailActions,
    activityEventDelegate: ActivityEventDelegate,
) {
    private val compositeDisposable = CompositeDisposable(
        actions.launchUserInfo.subscribe {
            activityEventDelegate.dispatchNavHostNavigate(TimelineEvent.Navigate.UserInfo(it.user))
        },
        actions.launchOriginalTweetUserInfo.subscribe {
            activityEventDelegate.dispatchNavHostNavigate(TimelineEvent.Navigate.UserInfo(it.user))
        },
        actions.launchMediaViewer.subscribe {
            activityEventDelegate.dispatchNavHostNavigate(TimelineEvent.Navigate.MediaViewer(it))
        },
    )

    fun clear() {
        compositeDisposable.clear()
    }
}

@Module
interface TweetDetailViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(TweetDetailViewModel::class)
        @ViewModelScope
        fun provideTweetDetailViewModel(
            tweetId: TweetId,
            eventDispatcher: EventDispatcher,
            viewStates: TweetDetailViewStates,
            tweetRepository: TweetRepository,
        ): ViewModel {
            return TweetDetailViewModel(tweetId, eventDispatcher, viewStates, tweetRepository)
        }
    }
}

@ViewModelScope
@Subcomponent(modules = [TweetDetailViewModelModule::class])
interface TweetDetailViewModelComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance tweetId: TweetId): TweetDetailViewModelComponent
    }

    val viewModelProvider: ViewModelProvider
}

@Module(subcomponents = [TweetDetailViewModelComponent::class])
interface TweetDetailViewModelComponentModule
