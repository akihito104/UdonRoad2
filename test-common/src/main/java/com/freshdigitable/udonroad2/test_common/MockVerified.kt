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

package com.freshdigitable.udonroad2.test_common

import io.mockk.confirmVerified
import io.mockk.mockk
import org.junit.rules.TestWatcher
import org.junit.runner.Description

typealias ExpectedVerify = () -> Unit

class MockVerified(
    private val mocks: List<Any>
) : TestWatcher() {

    private val expectedBlocks = mutableListOf<ExpectedVerify>()

    fun expected(block: ExpectedVerify) {
        expectedBlocks.add(block)
    }

    override fun succeeded(description: Description?) {
        super.succeeded(description)
        expectedBlocks.forEach { it() }
        confirmVerified(*mocks.toTypedArray())
    }
}

class MockVerified2<T>(
    val mock: T
) : TestWatcher() {

    companion object {
        inline fun <reified T> create(relaxed: Boolean = false): MockVerified2<T> {
            return MockVerified2(mockk(relaxed = relaxed))
        }
    }

    private val expectedBlocks = mutableListOf<ExpectedVerify>()

    fun expected(block: ExpectedVerify) {
        expectedBlocks.add(block)
    }

    override fun succeeded(description: Description?) {
        super.succeeded(description)
        expectedBlocks.forEach { it() }
        confirmVerified(mock as Any)
    }
}
