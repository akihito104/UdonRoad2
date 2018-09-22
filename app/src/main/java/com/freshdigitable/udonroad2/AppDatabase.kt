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

import androidx.room.Database
import androidx.room.RoomDatabase
import com.freshdigitable.udonroad2.tweet.TweetDao
import com.freshdigitable.udonroad2.tweet.TweetEntity
import com.freshdigitable.udonroad2.user.User
import com.freshdigitable.udonroad2.user.UserDao

@Database(
        entities = [
            TweetEntity::class,
            User::class
        ],
        exportSchema = false,
        version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tweetDao(): TweetDao

    abstract fun userDao(): UserDao
}
