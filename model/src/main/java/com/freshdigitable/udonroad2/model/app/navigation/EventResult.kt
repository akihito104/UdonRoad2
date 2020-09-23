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

package com.freshdigitable.udonroad2.model.app.navigation

import java.io.Serializable

data class EventResult<E : AppEvent, T>(
    val event: E,
    private val result: Result<T>
) : Serializable {
    val value: T? = result.getOrNull()
    val isSuccess: Boolean
        get() = result.isSuccess
    val isFailure: Boolean
        get() = result.isFailure
    val exception: Throwable?
        get() = result.exceptionOrNull()

    fun rethrow() {
        throw requireNotNull(exception)
    }

    companion object {
        fun <E : AppEvent, T> success(event: E, value: T): EventResult<E, T> {
            return EventResult(event, Result.success(value))
        }

        fun <E : AppEvent, T> failure(event: E, throwable: Throwable): EventResult<E, T> {
            return EventResult(event, Result.failure(throwable))
        }
    }
}
