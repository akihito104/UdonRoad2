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

package com.freshdigitable.udonroad2.model.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class AppExecutor(
    private val parentJob: Job = SupervisorJob(),
    val dispatcher: DispatcherProvider = DispatcherProvider(),
    coroutineScope: CoroutineScope = CoroutineScope(dispatcher.mainContext + parentJob),
) : CoroutineScope by coroutineScope

class DispatcherProvider(
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val exceptionHandler: CoroutineExceptionHandler? = null,
) {
    val mainContext: CoroutineContext
        get() = exceptionHandler?.plus(mainDispatcher) ?: mainDispatcher
    val ioContext: CoroutineContext
        get() = exceptionHandler?.plus(ioDispatcher) ?: ioDispatcher
}
