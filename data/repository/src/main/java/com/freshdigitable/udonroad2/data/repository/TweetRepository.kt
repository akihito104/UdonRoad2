package com.freshdigitable.udonroad2.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.restclient.TwitterModule
import com.freshdigitable.udonroad2.model.RepositoryScope
import com.freshdigitable.udonroad2.model.TweetListItem
import dagger.Module
import dagger.Provides
import javax.inject.Inject

class TweetRepository @Inject constructor(
    private val dao: TweetDao
) {
    fun getTweetItem(
        id: Long
    ): LiveData<TweetListItem?> = Transformations.map(dao.findTweetItem(id)) { it as TweetListItem? }
}

@Module(includes = [
    DaoModule::class,
    TwitterModule::class
])
object TweetRepositoryModule {
    @Provides
    @RepositoryScope
    @JvmStatic
    fun provideTweetRepository(dao: TweetDao): TweetRepository {
        return TweetRepository(dao)
    }
}
