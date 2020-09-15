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

package com.freshdigitable.udonroad2.data.restclient

import twitter4j.TwitterException

class AppTwitterException(exception: TwitterException) : Exception(exception) {
    val statusCode: Int = exception.statusCode
    val errorCode: Int = exception.errorCode
    val errorType: ErrorType? = findByCode(statusCode, errorCode)
    val exceptionCode: String = exception.exceptionCode

    companion object {
        private const val STATUS_FORBIDDEN: Int = 403

        private fun findByCode(statusCode: Int, errorCode: Int): ErrorType? {
            return ErrorType.values().firstOrNull {
                it.statusCode == statusCode && it.errorCode == errorCode
            }
        }
    }

    enum class ErrorType(
        val statusCode: Int,
        val errorCode: Int
    ) {
        // https://developer.twitter.com/ja/docs/basics/response-codes
        ALREADY_FAVORITED(STATUS_FORBIDDEN, 139),
        ALREADY_RETWEETED(STATUS_FORBIDDEN, 327),
    }
}
