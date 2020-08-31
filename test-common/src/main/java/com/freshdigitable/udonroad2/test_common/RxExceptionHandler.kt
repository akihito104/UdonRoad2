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

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.lang.Thread.UncaughtExceptionHandler

class RxExceptionHandler : TestWatcher() {
    private var defaultExceptionHandler: UncaughtExceptionHandler? = null
    private val exceptions = mutableListOf<Throwable>()

    override fun starting(description: Description?) {
        super.starting(description)
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            synchronized(exceptions) {
                exceptions += e
            }
        }
    }

    override fun succeeded(description: Description?) {
        super.succeeded(description)
        if (exceptions.isNotEmpty()) {
            val e = exceptions.first()
            exceptions.forEach { it.printStackTrace() }
            throw e
        }
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
    }
}
