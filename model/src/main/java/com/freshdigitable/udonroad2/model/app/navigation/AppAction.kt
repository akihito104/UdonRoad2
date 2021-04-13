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

package com.freshdigitable.udonroad2.model.app.navigation

import kotlinx.coroutines.flow.Flow

interface AppEventListener {
    fun dispatch()

    companion object {
        val empty = object : AppEventListener {
            override fun dispatch() = Unit
        }
    }
}

interface AppAction<E : AppEvent> : AppEventListener, Flow<E>

inline fun <reified E : AppEvent> EventDispatcher.toAction(
    event: E,
    crossinline prediction: (E) -> Boolean = { it == event },
): AppAction<E> = object : AppAction<E>, Flow<E> by this.toActionFlow(prediction) {
    override fun dispatch() {
        this@toAction.postEvent(event)
    }
}

interface AppEventListener1<T> {
    fun dispatch(t: T)

    companion object {
        fun <T> empty() = object : AppEventListener1<T> {
            override fun dispatch(t: T) = Unit
        }
    }
}

interface AppAction1<T, E : AppEvent> : AppEventListener1<T>, Flow<E>

inline fun <T, E : AppEvent> EventDispatcher.toListener(
    crossinline onEvent: (T) -> E,
): AppEventListener1<T> {
    return object : AppEventListener1<T> {
        override fun dispatch(t: T) {
            this@toListener.postEvent(onEvent(t))
        }
    }
}

inline fun <T, reified E : AppEvent> EventDispatcher.toAction(
    crossinline onEvent: (T) -> E,
): AppAction1<T, E> = object : AppAction1<T, E>,
    AppEventListener1<T> by this.toListener(onEvent),
    Flow<E> by this.toActionFlow() {}
