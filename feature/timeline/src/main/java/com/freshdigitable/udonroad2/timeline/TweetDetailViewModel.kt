package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.data.repository.RepositoryComponent
import com.freshdigitable.udonroad2.data.repository.TweetRepository
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import dagger.Module
import dagger.Provides
import javax.inject.Inject

class TweetDetailViewModel @Inject constructor(
    private val navigator: NavigationDispatcher,
    private val repository: TweetRepository
) : ViewModel() {

    private val targetId: MutableLiveData<Long> = MutableLiveData()
    val tweetItem: LiveData<TweetListItem?> = Transformations.switchMap(targetId) {
        repository.getTweetItem(it)
    }

    internal fun showTweetItem(id: Long) {
        targetId.value = id
    }

    fun onOriginalUserClicked() {
        val userId = tweetItem.value?.originalUser?.id ?: return
        navigator.postEvent(TimelineEvent.RetweetUserClicked(userId))
    }

    fun onBodyUserClicked() {
        val userId = tweetItem.value?.body?.user?.id ?: return
        navigator.postEvent(TimelineEvent.UserIconClicked(userId))
    }
}

@Module
object TweetDetailViewModelModule {
    @Provides
    @JvmStatic
    fun provideTweetDetailViewModel(
        navigator: NavigationDispatcher,
        repositories: RepositoryComponent.Builder
    ): TweetDetailViewModel {
        return TweetDetailViewModel(navigator, repositories.build().tweetRepository())
    }
}
