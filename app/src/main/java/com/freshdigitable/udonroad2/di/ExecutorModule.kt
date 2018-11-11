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

package com.freshdigitable.udonroad2.di

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import javax.inject.Singleton

@Module
class ExecutorModule {
    @Provides
    @Singleton
    fun provideAppExecutor(): AppExecutor = AppExecutor()
}

class AppExecutor {
    private val disk: Executor = Executor {
        diskAccess { it.run() }
    }

    val network: Executor = Executor {
        GlobalScope.launch(Dispatchers.Default) {
            it.run()
        }
    }

    fun diskIO(task: () -> Unit) = diskAccess(task)
}

fun diskAccess(task: () -> Unit) = GlobalScope.launch(Dispatchers.IO) {
    task()
}

suspend fun <T> networkAccess(callable: () -> T): T = coroutineScope {
    async(Dispatchers.Default) {
        callable()
    }.await()
}
