/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
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

import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnswerScope
import io.mockk.MockKMatcherScope
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import twitter4j.GeoLocation
import twitter4j.HashtagEntity
import twitter4j.MediaEntity
import twitter4j.Paging
import twitter4j.Place
import twitter4j.RateLimitStatus
import twitter4j.ResponseList
import twitter4j.Scopes
import twitter4j.Status
import twitter4j.SymbolEntity
import twitter4j.Twitter
import twitter4j.URLEntity
import twitter4j.User
import twitter4j.UserMentionEntity
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import java.util.Date

class TwitterRobot : TestWatcher() {
    private val twitter: Twitter by lazy {
        ApplicationProvider.getApplicationContext<TestApplicationBase>()
            .component
            .twitter
    }
    private val expected: MutableCollection<() -> Unit> = mutableListOf()

    fun setupSetOAuthAccessToken(block: MatcherScopedBlock<AccessToken?>) {
        every { twitter.setOAuthAccessToken(block()) } just runs
        expected.add { verify { twitter.setOAuthAccessToken(block()) } }
    }

    fun setupGetOAuthRequestToken(
        block: MatcherScopedBlock<String> = { "oob" },
        response: RequestToken
    ) {
        every { twitter.getOAuthRequestToken(block()) } returns response
        expected.add { verify { twitter.getOAuthRequestToken(block()) } }
    }

    fun setupGetOAuthAccessToken(
        requestTokenBlock: MatcherScopedBlock<RequestToken>,
        pinBlock: MatcherScopedBlock<String>,
        response: AccessToken
    ) {
        every { twitter.getOAuthAccessToken(requestTokenBlock(), pinBlock()) } returns response
        expected.add { verify { twitter.getOAuthAccessToken(requestTokenBlock(), pinBlock()) } }
    }

    fun setupGetHomeTimeline(
        pagingBlock: MatcherScopedBlock<Paging>? = null,
        response: List<Status>,
        answer: MockKAnswerScope<ResponseList<Status>, ResponseList<Status>>.() -> Unit = {}
    ) {
        val res = mockk<ResponseList<Status>>().apply {
            every { size } returns response.size
            every { iterator() } returns response.toMutableList().iterator()
        }
        if (pagingBlock == null) {
            every { twitter.homeTimeline } answers {
                answer()
                res
            }
            expected.add { verify { twitter.homeTimeline } }
        } else {
            every { twitter.getHomeTimeline(pagingBlock()) } answers {
                answer()
                res
            }
            expected.add { verify { twitter.getHomeTimeline(pagingBlock()) } }
        }
    }

    override fun succeeded(description: Description?) {
        super.succeeded(description)
        expected.forEach { it() }
        confirmVerified(twitter)
    }
}

typealias MatcherScopedBlock<T> = MockKMatcherScope.() -> T

fun createRequestToken(
    userId: Long,
    token: String,
    tokenSecret: String
): RequestToken {
    return RequestToken("$userId-$token", tokenSecret)
}

fun createStatus(
    id: Long,
    text: String,
    user: User,
    createdAt: Date,
    quotedStatus: Status? = null
): Status {
    return object : Status {
        override fun getUser(): User = user
        override fun getCreatedAt(): Date = createdAt
        override fun getId(): Long = id
        override fun getText(): String = text
        override fun getFavoriteCount(): Int = 0
        override fun getRetweetCount(): Int = 0
        override fun getQuotedStatusId(): Long = quotedStatus?.id ?: -1
        override fun getQuotedStatus(): Status? = quotedStatus
        override fun getSource(): String = "Twitter Web App"
        override fun getRetweetedStatus(): Status? = null
        override fun getInReplyToStatusId(): Long = -1
        override fun isFavorited(): Boolean = false
        override fun isRetweeted(): Boolean = false
        override fun isPossiblySensitive(): Boolean = false
        override fun getMediaEntities(): Array<MediaEntity> = emptyArray()

        override fun compareTo(other: Status): Int = this.createdAt.compareTo(other.createdAt)

        override fun getRateLimitStatus(): RateLimitStatus = TODO("Not yet implemented")
        override fun getAccessLevel(): Int = TODO("Not yet implemented")
        override fun getUserMentionEntities(): Array<UserMentionEntity> =
            TODO("Not yet implemented")

        override fun getURLEntities(): Array<URLEntity> = TODO("Not yet implemented")
        override fun getHashtagEntities(): Array<HashtagEntity> = TODO("Not yet implemented")
        override fun getSymbolEntities(): Array<SymbolEntity> = TODO("Not yet implemented")
        override fun getDisplayTextRangeStart(): Int = TODO("Not yet implemented")
        override fun getDisplayTextRangeEnd(): Int = TODO("Not yet implemented")
        override fun isTruncated(): Boolean = TODO("Not yet implemented")
        override fun getInReplyToUserId(): Long = TODO("Not yet implemented")
        override fun getInReplyToScreenName(): String = TODO("Not yet implemented")
        override fun getGeoLocation(): GeoLocation = TODO("Not yet implemented")
        override fun getPlace(): Place = TODO("Not yet implemented")
        override fun isRetweet(): Boolean = TODO("Not yet implemented")
        override fun getContributors(): LongArray = TODO("Not yet implemented")
        override fun isRetweetedByMe(): Boolean = TODO("Not yet implemented")
        override fun getCurrentUserRetweetId(): Long = TODO("Not yet implemented")
        override fun getLang(): String = TODO("Not yet implemented")
        override fun getScopes(): Scopes = TODO("Not yet implemented")
        override fun getWithheldInCountries(): Array<String> = TODO("Not yet implemented")
        override fun getQuotedStatusPermalink(): URLEntity = TODO("Not yet implemented")
    }
}

fun createUser(
    id: Long,
    name: String,
    screenName: String
): User {
    return object : User {
        override fun getId(): Long = id
        override fun getName(): String = name
        override fun getScreenName(): String = screenName
        override fun getDescription(): String = ""
        override fun getProfileImageURLHttps(): String = ""
        override fun getProfileBanner600x200URL(): String = ""
        override fun getFollowersCount(): Int = 0
        override fun getFriendsCount(): Int = 0
        override fun getStatusesCount(): Int = 0
        override fun getFavouritesCount(): Int = 0
        override fun getListedCount(): Int = 0
        override fun getProfileLinkColor(): String = "FFFFFF"
        override fun getLocation(): String = ""
        override fun getURL(): String = ""
        override fun isVerified(): Boolean = false
        override fun isProtected(): Boolean = false
        override fun getProfileImageURL(): String = ""

        override fun getProfileBanner1500x500URL(): String = TODO("Not yet implemented")
        override fun isProfileBackgroundTiled(): Boolean = TODO("Not yet implemented")
        override fun getLang(): String = TODO("Not yet implemented")
        override fun getStatus(): Status = TODO("Not yet implemented")
        override fun getProfileBackgroundColor(): String = TODO("Not yet implemented")
        override fun getProfileTextColor(): String = TODO("Not yet implemented")
        override fun getCreatedAt(): Date = TODO("Not yet implemented")
        override fun getUtcOffset(): Int = TODO("Not yet implemented")
        override fun getTimeZone(): String = TODO("Not yet implemented")
        override fun getProfileBackgroundImageURL(): String = TODO("Not yet implemented")
        override fun getProfileBackgroundImageUrlHttps(): String = TODO("Not yet implemented")
        override fun getProfileBannerURL(): String = TODO("Not yet implemented")
        override fun getProfileBannerRetinaURL(): String = TODO("Not yet implemented")
        override fun getProfileBannerIPadURL(): String = TODO("Not yet implemented")
        override fun getProfileBannerIPadRetinaURL(): String = TODO("Not yet implemented")
        override fun getProfileBannerMobileURL(): String = TODO("Not yet implemented")
        override fun getProfileBannerMobileRetinaURL(): String = TODO("Not yet implemented")
        override fun getProfileBanner300x100URL(): String = TODO("Not yet implemented")
        override fun isGeoEnabled(): Boolean = TODO("Not yet implemented")
        override fun isFollowRequestSent(): Boolean = TODO("Not yet implemented")
        override fun getDescriptionURLEntities(): Array<URLEntity> = TODO("Not yet implemented")
        override fun getURLEntity(): URLEntity = TODO("Not yet implemented")
        override fun getWithheldInCountries(): Array<String> = TODO("Not yet implemented")
        override fun getProfileSidebarFillColor(): String = TODO("Not yet implemented")
        override fun getProfileSidebarBorderColor(): String = TODO("Not yet implemented")
        override fun isProfileUseBackgroundImage(): Boolean = TODO("Not yet implemented")
        override fun isDefaultProfile(): Boolean = TODO("Not yet implemented")
        override fun isShowAllInlineMedia(): Boolean = TODO("Not yet implemented")
        override fun compareTo(other: User?): Int = TODO("Not yet implemented")
        override fun getRateLimitStatus(): RateLimitStatus = TODO("Not yet implemented")
        override fun getAccessLevel(): Int = TODO("Not yet implemented")
        override fun getEmail(): String = TODO("Not yet implemented")
        override fun isContributorsEnabled(): Boolean = TODO("Not yet implemented")
        override fun isTranslator(): Boolean = TODO("Not yet implemented")
        override fun getBiggerProfileImageURL(): String = TODO("Not yet implemented")
        override fun getMiniProfileImageURL(): String = TODO("Not yet implemented")
        override fun getOriginalProfileImageURL(): String = TODO("Not yet implemented")
        override fun get400x400ProfileImageURL(): String = TODO("Not yet implemented")
        override fun getBiggerProfileImageURLHttps(): String = TODO("Not yet implemented")
        override fun getMiniProfileImageURLHttps(): String = TODO("Not yet implemented")
        override fun getOriginalProfileImageURLHttps(): String = TODO("Not yet implemented")
        override fun get400x400ProfileImageURLHttps(): String = TODO("Not yet implemented")
        override fun isDefaultProfileImage(): Boolean = TODO("Not yet implemented")
    }
}
