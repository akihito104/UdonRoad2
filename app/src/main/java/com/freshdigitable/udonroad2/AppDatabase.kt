/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad2

import android.app.Application
import androidx.paging.DataSource
import androidx.room.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Database(
        entities = [
            Tweet::class,
            User::class
        ],
        exportSchema = false,
        version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tweetDao(): TweetDao
}

@Dao
abstract class TweetDao {
    @Query("SELECT * FROM Tweet ORDER BY id")
    abstract fun getHomeTimeline(): DataSource.Factory<Int, Tweet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addTweets(tweet: List<Tweet>)
}

@Module
class DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase =
        Room.databaseBuilder(app, AppDatabase::class.java, "app_data")
                .fallbackToDestructiveMigration()
                .build()

    @Provides
    fun providesTweetDao(db: AppDatabase): TweetDao = db.tweetDao()
}
