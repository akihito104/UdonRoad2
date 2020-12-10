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
import com.freshdigitable.udonroad2.data.db.converter.MediaIdConverter
import com.freshdigitable.udonroad2.data.db.converter.MediaTypeConverter
import com.freshdigitable.udonroad2.data.db.converter.MemberListIdConverter
import com.freshdigitable.udonroad2.data.db.converter.TimestampConverter
import com.freshdigitable.udonroad2.data.db.converter.TweetIdConverter
import com.freshdigitable.udonroad2.data.db.converter.UserIdConverter
import com.freshdigitable.udonroad2.data.db.dao.MediaDao
import com.freshdigitable.udonroad2.data.db.dao.MemberListDao
import com.freshdigitable.udonroad2.data.db.dao.RelationshipDao
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.db.dao.UrlDao
import com.freshdigitable.udonroad2.data.db.dao.UserDao
import com.freshdigitable.udonroad2.data.db.dao.UserReplyEntityDao
import com.freshdigitable.udonroad2.data.db.dao.VideoValiantDao
import com.freshdigitable.udonroad2.data.db.dbview.MediaDbView
import com.freshdigitable.udonroad2.data.db.dbview.MemberListDbView
import com.freshdigitable.udonroad2.data.db.dbview.TweetDbView
import com.freshdigitable.udonroad2.data.db.dbview.TweetListItemDbView
import com.freshdigitable.udonroad2.data.db.dbview.TweetingUser
import com.freshdigitable.udonroad2.data.db.dbview.UserListDbView
import com.freshdigitable.udonroad2.data.db.entity.MediaEntity
import com.freshdigitable.udonroad2.data.db.entity.MediaUrlEntity
import com.freshdigitable.udonroad2.data.db.entity.MemberListEntity
import com.freshdigitable.udonroad2.data.db.entity.MemberListListEntity
import com.freshdigitable.udonroad2.data.db.entity.RelationshipEntity
import com.freshdigitable.udonroad2.data.db.entity.StructuredTweetEntity
import com.freshdigitable.udonroad2.data.db.entity.TweetEntityDb
import com.freshdigitable.udonroad2.data.db.entity.TweetListEntity
import com.freshdigitable.udonroad2.data.db.entity.UrlEntity
import com.freshdigitable.udonroad2.data.db.entity.UserEntity
import com.freshdigitable.udonroad2.data.db.entity.UserListEntity
import com.freshdigitable.udonroad2.data.db.entity.UserReplyEntityDb
import com.freshdigitable.udonroad2.data.db.entity.VideoValiantEntity

@Database(
    entities = [
        TweetEntityDb::class,
        StructuredTweetEntity::class,
        TweetListEntity::class,
        UserEntity::class,
        UserListEntity::class,
        MemberListEntity::class,
        MemberListListEntity::class,
        RelationshipEntity::class,
        UrlEntity::class,
        MediaEntity::class,
        MediaUrlEntity::class,
        VideoValiantEntity::class,
        UserReplyEntityDb::class,
    ],
    views = [
        TweetDbView::class,
        TweetListItemDbView::class,
        TweetingUser::class,
        UserListDbView::class,
        MemberListDbView::class,
        MediaDbView::class
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
    MemberListIdConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tweetDao(): TweetDao

    abstract fun userDao(): UserDao

    abstract fun memberListDao(): MemberListDao

    abstract fun relationshipDao(): RelationshipDao

    abstract fun mediaDao(): MediaDao

    abstract fun videoValiantDao(): VideoValiantDao

    abstract fun urlDao(): UrlDao

    abstract fun userReplyDao(): UserReplyEntityDao
}

interface AppTypeConverter<E, I> {
    @TypeConverter
    fun toItem(v: I): E

    @TypeConverter
    fun toEntity(v: E): I
}
