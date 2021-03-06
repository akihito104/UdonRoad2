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

import io.mockk.Answer
import io.mockk.Call
import io.mockk.MockKAnswerScope
import io.mockk.MockKMatcherScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

typealias ExpectedVerify = () -> Unit

class MockVerified<T>(
    val mock: T
) : TestRule {

    companion object {
        inline fun <reified T> create(relaxed: Boolean = false): MockVerified<T> {
            return MockVerified(mockk(relaxed = relaxed))
        }
    }

    private val expectedBlocks = mutableListOf<ExpectedVerify>()

    fun expected(block: ExpectedVerify) {
        expectedBlocks.add(block)
    }

    fun <R> setupResponseWithVerify(
        target: MatcherScopedBlock<R>,
        res: R,
        alsoOnAnswer: AnswerScopedBlock<R, R> = {},
    ) {
        every(target) answers {
            alsoOnAnswer()
            res
        }
        expectedBlocks.add { verify { target() } }
    }

    fun <R> coSetupResponseWithVerify(
        target: MatcherScopedSuspendBlock<R>,
        res: R,
        alsoOnAnswer: () -> Unit = {},
    ) {
        coSetupWithVerify(target) {
            alsoOnAnswer()
            res
        }
    }

    fun <R> coSetupThrowWithVerify(
        target: MatcherScopedSuspendBlock<R>,
        throwable: Throwable,
        alsoOnAnswer: () -> Unit = {},
    ) {
        coSetupWithVerify(target) {
            alsoOnAnswer()
            throw throwable
        }
    }

    private fun <R> coSetupWithVerify(
        target: MatcherScopedSuspendBlock<R>,
        answer: () -> R
    ) {
        coEvery(target).answers(object : Answer<R> {
            override fun answer(call: Call): R = answer()
        })
        expectedBlocks.add { coVerify(exactly = 1) { target() } } // TODO make `exactly` argument
    }

    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                base.evaluate()
                verify()
            }
        }
    }

    private fun verify() {
        expectedBlocks.forEach { it() }
        confirmVerified(mock as Any)
    }
}

typealias MatcherScopedBlock<R> = MockKMatcherScope.() -> R
typealias MatcherScopedSuspendBlock<R> = suspend MockKMatcherScope.() -> R
typealias AnswerScopedBlock<T, B> = MockKAnswerScope<T, B>.() -> Unit
