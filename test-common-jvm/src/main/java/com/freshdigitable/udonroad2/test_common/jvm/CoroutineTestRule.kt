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

package com.freshdigitable.udonroad2.test_common.jvm

import com.freshdigitable.udonroad2.model.app.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineExceptionHandler
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@ExperimentalCoroutinesApi
class CoroutineTestRule : TestWatcher() {
    val testCoroutineDispatcher = TestCoroutineDispatcher()
    private val exceptionHandler = TestCoroutineExceptionHandler()
    val coroutineContextProvider = DispatcherProvider(
        testCoroutineDispatcher,
        testCoroutineDispatcher,
        exceptionHandler
    )

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) {
        runBlockingTest(coroutineContextProvider.mainContext, block)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        exceptionHandler.cleanupTestCoroutines()
        testCoroutineDispatcher.cleanupTestCoroutines()
        Dispatchers.resetMain()
    }
}
