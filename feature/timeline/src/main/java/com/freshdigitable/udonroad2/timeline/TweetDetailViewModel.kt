package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.data.repository.RepositoryComponent
import com.freshdigitable.udonroad2.data.repository.TweetRepository
import com.freshdigitable.udonroad2.model.TweetListItem
import dagger.Module
import dagger.Provides
import javax.inject.Inject

class TweetDetailViewModel @Inject constructor(
    private val repository: TweetRepository
) : ViewModel() {

    private val targetId: MutableLiveData<Long> = MutableLiveData()
    val tweetItem: LiveData<TweetListItem?> = Transformations.switchMap(targetId) {
        repository.getTweetItem(it)
    }

    internal fun showTweetItem(id: Long) {
        targetId.value = id
    }
}

@Module
object TweetDetailViewModelModule {
    @Provides
    @JvmStatic
    fun provideTweetDetailViewModel(
        repositories: RepositoryComponent.Builder
    ): TweetDetailViewModel {
        return TweetDetailViewModel(repositories.build().tweetRepository())
    }
}
