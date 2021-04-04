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
    fun onEvent()
}

interface AppAction<E : AppEvent> : AppEventListener, Flow<E>

inline fun <reified E : AppEvent> EventDispatcher.toAction(
    event: E,
): AppAction<E> = object : AppAction<E>, Flow<E> by this.toActionFlow() {
    override fun onEvent() {
        this@toAction.postEvent(event)
    }
}

interface AppEventListener1<T> {
    fun onEvent(t: T)
}

interface AppAction1<T, E : AppEvent> : AppEventListener1<T>, Flow<E>

inline fun <T, reified E : AppEvent> EventDispatcher.toAction(
    crossinline onEvent: (T) -> E,
): AppAction1<T, E> = object : AppAction1<T, E>, Flow<E> by this.toActionFlow() {
    override fun onEvent(t: T) {
        this@toAction.postEvent(onEvent(t))
    }
}
