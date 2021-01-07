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

package com.freshdigitable.udonroad2.test

import android.app.Activity
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage

fun createIdlingResource(name: String, block: () -> Boolean): IdlingResource {
    return object : IdlingResource {
        override fun getName() = name

        override fun isIdleNow(): Boolean {
            val isIdle = block()
            if (isIdle) {
                callback?.onTransitionToIdle()
            }
            return isIdle
        }

        private var callback: IdlingResource.ResourceCallback? = null
        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
            this.callback = callback
        }
    }
}

fun waitWithIdlingResource(name: String, block: () -> Boolean, afterTask: () -> Unit) {
    val idlingRegistry = IdlingRegistry.getInstance()
    val resource = createIdlingResource(name, block)
    try {
        idlingRegistry.register(resource)
        afterTask()
    } finally {
        idlingRegistry.unregister(resource)
    }
}

inline fun <reified T : Activity> waitForActivity(
    stage: Stage = Stage.RESUMED,
    name: String = "wait_for_${T::class.java.simpleName}_in_${stage.name}",
    crossinline onActivity: (T) -> Boolean = { true },
    noinline afterTask: () -> Unit
) {
    waitWithIdlingResource(
        name,
        {
            val a = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(stage)
                .firstOrNull { it is T } ?: return@waitWithIdlingResource false
            return@waitWithIdlingResource onActivity(a as T)
        },
        afterTask
    )
}
