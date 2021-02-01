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
import com.freshdigitable.udonroad2.data.db.converter.CustomTimelineIdConverter
import com.freshdigitable.udonroad2.data.db.converter.MediaIdConverter
import com.freshdigitable.udonroad2.data.db.converter.MediaTypeConverter
import com.freshdigitable.udonroad2.data.db.converter.TimestampConverter
import com.freshdigitable.udonroad2.data.db.converter.TweetIdConverter
import com.freshdigitable.udonroad2.data.db.converter.UserIdConverter
import com.freshdigitable.udonroad2.data.db.dao.CustomTimelineDao
import com.freshdigitable.udonroad2.data.db.dao.MediaDao
import com.freshdigitable.udonroad2.data.db.dao.ReactionsDao
import com.freshdigitable.udonroad2.data.db.dao.RelationshipDao
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.db.dao.UrlDao
import com.freshdigitable.udonroad2.data.db.dao.UserDao
import com.freshdigitable.udonroad2.data.db.dao.UserReplyEntityDao
import com.freshdigitable.udonroad2.data.db.dao.VideoValiantDao
import com.freshdigitable.udonroad2.data.db.dbview.CustomTimelineListItemDb
import com.freshdigitable.udonroad2.data.db.dbview.TweetItemMediaDbView
import com.freshdigitable.udonroad2.data.db.dbview.TweetListItemDbView
import com.freshdigitable.udonroad2.data.db.dbview.UserListDbView
import com.freshdigitable.udonroad2.data.db.entity.CustomTimelineDb
import com.freshdigitable.udonroad2.data.db.entity.CustomTimelineListDb
import com.freshdigitable.udonroad2.data.db.entity.Favorited
import com.freshdigitable.udonroad2.data.db.entity.ListDao
import com.freshdigitable.udonroad2.data.db.entity.ListEntityDb
import com.freshdigitable.udonroad2.data.db.entity.ListIdConverter
import com.freshdigitable.udonroad2.data.db.entity.MediaDbEntity
import com.freshdigitable.udonroad2.data.db.entity.MediaUrlEntity
import com.freshdigitable.udonroad2.data.db.entity.RelationshipEntity
import com.freshdigitable.udonroad2.data.db.entity.Retweeted
import com.freshdigitable.udonroad2.data.db.entity.TweetElementDb
import com.freshdigitable.udonroad2.data.db.entity.TweetEntityDb
import com.freshdigitable.udonroad2.data.db.entity.TweetListEntity
import com.freshdigitable.udonroad2.data.db.entity.UrlEntity
import com.freshdigitable.udonroad2.data.db.entity.UserEntityDb
import com.freshdigitable.udonroad2.data.db.entity.UserListEntity
import com.freshdigitable.udonroad2.data.db.entity.UserReplyEntityDb
import com.freshdigitable.udonroad2.data.db.entity.VideoValiantEntity

@Database(
    entities = [
        TweetElementDb::class,
        TweetEntityDb::class,
        TweetListEntity::class,
        Favorited::class,
        Retweeted::class,
        UserEntityDb::class,
        UserListEntity::class,
        CustomTimelineDb::class,
        CustomTimelineListDb::class,
        RelationshipEntity::class,
        UrlEntity::class,
        MediaDbEntity::class,
        MediaUrlEntity::class,
        VideoValiantEntity::class,
        UserReplyEntityDb::class,
        ListEntityDb::class,
    ],
    views = [
        TweetListItemDbView::class,
        UserListDbView::class,
        CustomTimelineListItemDb::class,
        TweetItemMediaDbView::class
    ],
    exportSchema = false,
    version = 1
)
@TypeConverters(
    TimestampConverter::class,
    MediaIdConverter::class,
    MediaTypeConverter::class,
    TweetIdConverter::class,
    UserIdConverter::class,
    CustomTimelineIdConverter::class,
    ListIdConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tweetDao(): TweetDao
    internal abstract val reactionsDao: ReactionsDao

    internal abstract fun userDao(): UserDao

    abstract fun customTimelineDao(): CustomTimelineDao

    internal abstract fun relationshipDao(): RelationshipDao

    abstract fun mediaDao(): MediaDao

    abstract fun videoValiantDao(): VideoValiantDao

    abstract fun urlDao(): UrlDao

    internal abstract fun userReplyDao(): UserReplyEntityDao

    abstract fun listDao(): ListDao
}

interface AppTypeConverter<E, I> {
    @TypeConverter
    fun toItem(v: I): E

    @TypeConverter
    fun toEntity(v: E): I
}
