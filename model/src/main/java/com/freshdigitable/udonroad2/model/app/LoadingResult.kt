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

package com.freshdigitable.udonroad2.model.app

import timber.log.Timber
import java.io.IOException

sealed class LoadingResult<out T> {
    object Started : LoadingResult<Nothing>()
    data class Loaded<T>(val value: T) : LoadingResult<T>()
    data class Failed(
        val errorType: AppErrorType,
        val exception: Throwable,
    ) : LoadingResult<Nothing>()
}

inline fun <T, R> T.load(block: T.() -> R): LoadingResult<R> {
    return this.runCatching(block).fold(
        onSuccess = { LoadingResult.Loaded(it) },
        onFailure = {
            Timber.tag("LoadingResult").e(it)
            val cause = when (it) {
                is AppTwitterException -> it.errorType ?: RecoverableErrorType.UNKNOWN
                is IOException -> RecoverableErrorType.API_ACCESS_TROUBLE // ???
                else -> throw it
            }
            LoadingResult.Failed(errorType = cause, exception = it)
        }
    )
}
