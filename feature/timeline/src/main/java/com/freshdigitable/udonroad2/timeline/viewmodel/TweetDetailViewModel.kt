package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.app.di.FragmentScope
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.Tweet
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Inject

class TweetDetailViewModel @Inject constructor(
    private val eventDispatcher: EventDispatcher,
    private val repository: TweetRepository
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
            tweetRepository: TweetRepository
        ): TweetDetailViewModel {
            return TweetDetailViewModel(eventDispatcher, tweetRepository)
        }
    }
}
