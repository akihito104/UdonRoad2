package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.app.di.FragmentScope
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.tweet.Tweet
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class TweetDetailViewModel(
    private val eventDispatcher: EventDispatcher,
    private val viewStates: TweetDetailViewStates,
    private val repository: TweetRepository,
) : TweetListItemClickListener, ViewModel() {

    private val targetId: MutableLiveData<TweetId> = MutableLiveData()
    val tweetItem: LiveData<TweetListItem?> = targetId.switchMap {
        repository.getTweetItem(it)
    }

    internal fun showTweetItem(id: TweetId) {
        targetId.value = id
    }

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
) : UserIconClickedAction by UserIconClickedAction.create(eventDispatcher) {
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
        }
    )

    fun clear() {
        compositeDisposable.clear()
    }
}

@Module
interface TweetDetailViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(TweetDetailViewModel::class)
    fun bindTweetDetailViewModel(viewModel: TweetDetailViewModel): ViewModel

    companion object {
        @Provides
        @FragmentScope
        fun provideTweetDetailViewModel(
            eventDispatcher: EventDispatcher,
            viewStates: TweetDetailViewStates,
            tweetRepository: TweetRepository,
        ): TweetDetailViewModel {
            return TweetDetailViewModel(eventDispatcher, viewStates, tweetRepository)
        }
    }
}
