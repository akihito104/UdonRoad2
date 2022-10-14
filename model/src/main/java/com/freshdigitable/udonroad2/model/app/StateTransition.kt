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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

typealias UpdateFun<S> = suspend (S) -> S
typealias ScanFun<S, E> = suspend (S, E) -> S

fun <S> stateSourceBuilder(
    init: S,
    vararg updateSource: Flow<UpdateFun<S>>,
): Flow<S> {
    require(updateSource.isNotEmpty())
    val builder = StateSourceBuilder(initBlock = { init }, updateSource = updateSource)
    return builder.build()
}

fun <S> stateSourceBuilder(
    initBlock: suspend () -> S,
    scope: StateSourceBuilderScope<S>.() -> Unit = {},
): Flow<S> {
    val builder = StateSourceBuilder(initBlock)
    builder.scope()
    return builder.build()
}

interface StateSourceBuilderScope<S> {
    fun <R> flatMap(flow: Flow<S>.() -> Flow<R>, scan: ScanFun<S, R>)
    fun <E> eventOf(flow: Flow<E>, scan: ScanFun<S, E>)
}

internal class StateSourceBuilder<S>(
    private val initBlock: suspend () -> S,
    private vararg val updateSource: Flow<UpdateFun<S>>,
) : StateSourceBuilderScope<S> {
    private val updateWithState = mutableListOf<FlatMapWithState<S, *>>()
    private val updaters = mutableListOf<Updater<S, *>>()

    override fun <E> eventOf(flow: Flow<E>, scan: ScanFun<S, E>) {
        updaters.add(Updater(flow, scan))
    }

    override fun <R> flatMap(
        flow: (Flow<S>) -> Flow<R>,
        scan: ScanFun<S, R>,
    ) {
        updateWithState.add(FlatMapWithState(flow, scan))
    }

    fun build(): Flow<S> {
        val initialized = AtomicBoolean(false)
        val stateFlow = MutableStateFlow<S?>(null)
        val updateSources = merge(
            *updateSource,
            *updaters.map { it.create() }.toTypedArray(),
            *updateWithState.map { s -> s.create(stateFlow.mapNotNull { it }) }.toTypedArray()
        )
        updaters.clear()
        updateWithState.clear()
        return flow {
            if (initialized.compareAndSet(false, true)) {
                val state: S = initBlock()
                stateFlow.value = state
                Timber.tag("StateTransition").d("emit(init)")
                emit(state)
            }

            updateSources.collect {
                val current = requireNotNull(stateFlow.value)
                val next = it(current)
                if (next != current) {
                    stateFlow.value = next
                    emit(next)
                }
            }
        }.onEach {
            Timber.tag("StateTransition").d("onEach: $it")
        }
    }

    class FlatMapWithState<S, R>(
        private val flow: Flow<S>.() -> Flow<R>,
        private val scan: ScanFun<S, R>,
    ) {
        fun create(stateFlow: Flow<S>): Flow<UpdateFun<S>> = flow(stateFlow).onEvent(scan)
    }

    class Updater<S, E>(
        private val flow: Flow<E>,
        private val scan: ScanFun<S, E>,
    ) {
        fun create(): Flow<UpdateFun<S>> = flow.onEvent(scan)
    }
}

inline fun <E, S> Flow<E>.onEvent(
    crossinline update: ScanFun<S, E>,
): Flow<UpdateFun<S>> = this.mapLatest { e -> { update(it, e) } }
