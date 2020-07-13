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

import android.app.Instrumentation
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import io.mockk.MockKMatcherScope
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken

class TwitterRobot : TestWatcher() {
    private val twitter = ApplicationProvider.getApplicationContext<TestApplicationBase>()
        .component
        .twitter
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

    fun setupGetHomeTimeline(response: List<Status>) {
        every { twitter.homeTimeline } returns mockk<ResponseList<Status>>().apply {
            every { size } returns response.size
            every { iterator() } returns response.toMutableList().iterator()
        }
        expected.add { verify { twitter.homeTimeline } }
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
    tokenSecret: String,
    authorizationUrl: String
): RequestToken {
    return mockk<RequestToken>().apply {
        every { this@apply.token } returns "$userId-$token"
        every { this@apply.tokenSecret } returns tokenSecret
        every { authorizationURL } returns authorizationUrl
    }
}

fun intendingToAuthorizationUrl(url: String) {
    intending(hasData(Uri.parse(url))).respondWithFunction {
        Instrumentation.ActivityResult(0, null)
    }
}
