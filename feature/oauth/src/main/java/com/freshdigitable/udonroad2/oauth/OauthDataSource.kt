/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.oauth

import android.content.Context
import androidx.paging.PositionalDataSource
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId

internal class OauthDataSource(context: Context) : PositionalDataSource<OauthItem>() {

    private val user = OauthUser(
        id = UserId(1),
        name = "aoeliyakei",
        screenName = "aoeliyakei",
        iconUrl = ""
    )
    private val items = listOf(
        OauthItem(
            originalId = TweetId(2),
            originalUser = user,
            body = OauthTweetElement(
                id = TweetId(2),
                user = user,
                text = context.getString(R.string.oauth_demo_tweet),
                retweetCount = 0,
                favoriteCount = 0,
                source = "aoeliyakei"
            )
        ),
        OauthItem(
            originalId = TweetId(1),
            originalUser = user,
            body = OauthTweetElement(
                id = TweetId(1),
                user = user,
                text = "",
                retweetCount = 0,
                favoriteCount = 0,
                source = "aoeliyakei"
            )
        )
    )

    override fun loadRange(
        params: LoadRangeParams,
        callback: LoadRangeCallback<OauthItem>
    ) {
        callback.onResult(items)
    }

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<OauthItem>
    ) {
        callback.onResult(items, 0)
    }
}
