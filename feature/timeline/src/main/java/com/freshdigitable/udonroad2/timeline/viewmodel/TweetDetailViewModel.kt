package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.data.impl.TwitterCardRepository
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.TwitterCard
import com.freshdigitable.udonroad2.model.UrlItem
import com.freshdigitable.udonroad2.model.app.AppTwitterException.ErrorType
import com.freshdigitable.udonroad2.model.app.LoadingResult
import com.freshdigitable.udonroad2.model.app.RecoverableErrorType
import com.freshdigitable.udonroad2.model.app.load
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectStream
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener1
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.model.tweet.DetailTweetListItem
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.tweet.TweetListItem.Companion.permalink
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetMediaEventListener
import com.freshdigitable.udonroad2.timeline.TweetMediaItemViewModel
import com.freshdigitable.udonroad2.timeline.TweetMediaViewModelSource
import com.freshdigitable.udonroad2.timeline.UserIconClickListener
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class TweetDetailViewModel(
    viewStates: TweetDetailViewStates,
    userIconViewModelSource: UserIconViewModelSource,
    mediaViewModelSource: TweetMediaViewModelSource,
    coroutineContext: CoroutineContext? = null,
) : TweetDetailEventListener by viewStates,
    UserIconClickListener by userIconViewModelSource,
    TweetMediaItemViewModel,
    TweetMediaEventListener by mediaViewModelSource,
    ActivityEffectStream,
    ViewModel() {
    private val coroutineContext: CoroutineContext =
        coroutineContext ?: viewModelScope.coroutineContext

    val state: LiveData<State> = viewStates.viewModelState.asLiveData(this.coroutineContext)
    override val mediaState: LiveData<TweetMediaItemViewModel.State> =
        mediaViewModelSource.mediaState.asLiveData(this.coroutineContext)
    override val effect: Flow<AppEffect> = merge(
        viewStates.effect,
        userIconViewModelSource.navEvent,
        mediaViewModelSource.effect
    )

    interface State {
        val tweetItem: DetailTweetListItem?
        val twitterCard: TwitterCard?
    }
}

sealed class DetailEvent : AppEvent {
    internal data class SpanClicked(val urlItem: UrlItem) : DetailEvent()
    internal data class TwitterCardClicked(val card: TwitterCard) : DetailEvent()
    internal data class TwitterIconClicked(val item: TweetListItem) : DetailEvent()
}

data class ExternalAppNavigation(
    val url: String,
    val appUrl: String? = null,
) : AppEffect.Navigation

interface SpanClickListener {
    fun onSpanClicked(urlItem: UrlItem)
}

interface TweetDetailEventListener : SpanClickListener {
    fun onOriginalUserClicked(user: TweetUserItem)
    fun onBodyUserClicked(user: TweetUserItem)
    fun onTwitterCardClicked(card: TwitterCard)
    val launchExternalTwitterApp: AppEventListener1<TweetListItem>
}

internal class TweetDetailActions @Inject constructor(
    private val eventDispatcher: EventDispatcher,
) : TweetDetailEventListener {
    val launchOriginalTweetUserInfo =
        eventDispatcher.toActionFlow<TimelineEvent.RetweetUserClicked>()
    val launchAppForCard = eventDispatcher.toActionFlow<DetailEvent.TwitterCardClicked>()
    val launchExternalApp = eventDispatcher.toActionFlow<DetailEvent.SpanClicked>()

    override fun onOriginalUserClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.RetweetUserClicked(user))
    }

    override fun onBodyUserClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override fun onSpanClicked(urlItem: UrlItem) {
        eventDispatcher.postEvent(DetailEvent.SpanClicked(urlItem))
    }

    override fun onTwitterCardClicked(card: TwitterCard) {
        eventDispatcher.postEvent(DetailEvent.TwitterCardClicked(card))
    }

    override val launchExternalTwitterApp = eventDispatcher.toAction { item: TweetListItem ->
        DetailEvent.TwitterIconClicked(item)
    }
}

internal class TweetDetailViewStates @Inject constructor(
    tweetId: TweetId,
    actions: TweetDetailActions,
    getTweetDetailItem: GetTweetDetailItemUseCase,
    twitterCardRepository: TwitterCardRepository,
) : TweetDetailEventListener by actions,
    ActivityEffectStream {

    internal val viewModelState: Flow<TweetDetailViewModel.State> = stateSourceBuilder(
        { ViewModelState() }
    ) {
        eventOf(getTweetDetailItem(tweetId)) { s, loadingItem ->
            if (s.isTweetItemDeleted) {
                return@eventOf s
            }
            when (loadingItem) {
                is LoadingResult.Started -> s
                is LoadingResult.Loaded -> {
                    val item = loadingItem.value
                    s.copy(tweetItem = item, isTweetItemDeleted = item == null)
                }
                is LoadingResult.Failed -> {
                    when (loadingItem.errorType) {
                        ErrorType.TWEET_NOT_FOUND ->
                            s.copy(tweetItem = null, isTweetItemDeleted = false)
                        RecoverableErrorType.API_ACCESS_TROUBLE -> s // TODO feedback
                        else -> throw IllegalStateException(loadingItem.exception)
                    }
                }
            }
        }
        flatMap(
            flow = {
                this.mapNotNull { it.urlForCard }
                    .distinctUntilChanged()
                    .flatMapLatest { twitterCardRepository.getTwitterCardSource(it) }
            }
        ) { s, card -> s.copy(twitterCard = card) }
    }

    override val effect: Flow<AppEffect> = merge(
        actions.launchOriginalTweetUserInfo.mapLatest { TimelineEffect.Navigate.UserInfo(it.user) },
        actions.launchAppForCard.mapLatest {
            ExternalAppNavigation(url = it.card.url, appUrl = it.card.appUrl)
        },
        actions.launchExternalApp.mapLatest {
            ExternalAppNavigation(url = it.urlItem.url)
        },
        actions.launchExternalTwitterApp.mapLatest {
            ExternalAppNavigation(url = it.item.permalink)
        },
    )

    private data class ViewModelState(
        override val tweetItem: DetailTweetListItem? = null,
        val isTweetItemDeleted: Boolean = false,
        override val twitterCard: TwitterCard? = null,
    ) : TweetDetailViewModel.State {
        val urlForCard: String?
            get() = tweetItem?.body?.urlItems?.firstOrNull()?.expandedUrl
    }
}

class GetTweetDetailItemUseCase @Inject constructor(
    private val repository: TweetRepository,
) {
    operator fun invoke(tweetId: TweetId): Flow<LoadingResult<DetailTweetListItem?>> {
        return flow {
            emit(LoadingResult.Started)

            repository.getDetailTweetItemSource(tweetId).collect { item ->
                when {
                    item != null -> emit(LoadingResult.Loaded(item))
                    else -> {
                        val state = repository.load { findDetailTweetItem(tweetId) }
                        emit(state)
                    }
                }
            }
        }
    }
}
