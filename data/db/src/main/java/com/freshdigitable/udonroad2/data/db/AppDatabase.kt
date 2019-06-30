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

package com.freshdigitable.udonroad2.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.freshdigitable.udonroad2.data.db.dao.MemberListDao
import com.freshdigitable.udonroad2.data.db.dao.MemberListListEntity
import com.freshdigitable.udonroad2.data.db.dao.RelationshipDao
import com.freshdigitable.udonroad2.data.db.dao.StructuredTweetEntity
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.db.dao.TweetListEntity
import com.freshdigitable.udonroad2.data.db.dao.UserDao
import com.freshdigitable.udonroad2.data.db.dao.UserListEntity
import com.freshdigitable.udonroad2.data.db.dbview.MemberListDbView
import com.freshdigitable.udonroad2.data.db.dbview.Tweet
import com.freshdigitable.udonroad2.data.db.dbview.TweetListItem
import com.freshdigitable.udonroad2.data.db.dbview.TweetingUser
import com.freshdigitable.udonroad2.data.db.dbview.UserListDbView
import com.freshdigitable.udonroad2.data.db.entity.MemberListEntity
import com.freshdigitable.udonroad2.data.db.entity.RelationshipEntity
import com.freshdigitable.udonroad2.data.db.entity.TweetEntityDb
import com.freshdigitable.udonroad2.data.db.entity.UserEntity
import org.threeten.bp.Instant

@Database(
    entities = [
        TweetEntityDb::class,
        StructuredTweetEntity::class,
        TweetListEntity::class,
        UserEntity::class,
        UserListEntity::class,
        MemberListEntity::class,
        MemberListListEntity::class,
        RelationshipEntity::class
    ],
    views = [
        Tweet::class,
        TweetListItem::class,
        TweetingUser::class,
        UserListDbView::class,
        MemberListDbView::class
    ],
    exportSchema = false,
    version = 1
)
@TypeConverters(TimestampConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tweetDao(): TweetDao

    abstract fun userDao(): UserDao

    abstract fun memberListDao(): MemberListDao

    abstract fun relationshipDao(): RelationshipDao
}

class TimestampConverter {
    @TypeConverter
    fun serialize(time: Instant) = time.toEpochMilli()

    @TypeConverter
    fun deserialize(time: Long): Instant = Instant.ofEpochMilli(time)
}
