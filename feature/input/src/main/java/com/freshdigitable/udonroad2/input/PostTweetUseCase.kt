/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.input

import com.freshdigitable.udonroad2.data.impl.TweetInputRepository
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppFilePath
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject

class PostTweetUseCase @Inject constructor(
    private val repository: TweetInputRepository,
    private val createQuoteText: CreateQuoteTextUseCase,
) {
    operator fun invoke(
        tweet: InputTweet,
        idlingState: InputTaskState,
    ): Flow<InputTaskState> = flow {
        emit(InputTaskState.SENDING)

        val mediaIds = tweet.media.map { repository.uploadMedia(it) }
        val quoteText = tweet.quote?.let { createQuoteText(it) }
        val text = if (quoteText == null) tweet.text else "${tweet.text} $quoteText"

        kotlin.runCatching {
            repository.post(text, mediaIds, tweet.reply)
        }.onSuccess {
            emit(InputTaskState.SUCCEEDED)
            emit(idlingState)
        }.onFailure { exception ->
            if (exception is AppTwitterException || exception is IOException) {
                emit(InputTaskState.FAILED)
            } else {
                throw exception
            }
        }
    }
}

interface InputTweet {
    val text: String
    val reply: TweetId?
    val quote: TweetId?
    val media: List<AppFilePath>
}
