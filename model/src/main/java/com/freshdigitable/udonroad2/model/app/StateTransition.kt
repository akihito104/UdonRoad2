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

package com.freshdigitable.udonroad2.model.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

typealias UpdateFun<S> = suspend (S) -> S
typealias ScanFun<S, E> = suspend (S, E) -> S

fun <S> stateSourceBuilder(
    init: S,
    vararg updateSource: Flow<UpdateFun<S>>
): Flow<S> {
    check(updateSource.isNotEmpty())
    return merge(*updateSource).scan(init) { state, trans -> trans(state) }
}

inline fun <E, S> Flow<E>.onEvent(crossinline update: ScanFun<S, E>): Flow<UpdateFun<S>> =
    this.map { e -> { s -> update(s, e) } }
