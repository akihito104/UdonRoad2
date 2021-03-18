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

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

typealias UpdateFun<S> = suspend (S) -> S
typealias ScanFun<S, E> = suspend (S, E) -> S

fun <S> stateSourceBuilder(
    init: S,
    vararg updateSource: Flow<UpdateFun<S>>
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
    private vararg val updateSource: Flow<UpdateFun<S>>
) : StateSourceBuilderScope<S> {
    private val updateWithState = mutableListOf<FlatMapWithState<S, *>>()
    private val updaters = mutableListOf<Updater<S, *>>()

    override fun <E> eventOf(flow: Flow<E>, scan: ScanFun<S, E>) {
        updaters.add(Updater(flow, scan))
    }

    override fun <R> flatMap(
        flow: (Flow<S>) -> Flow<R>,
        scan: ScanFun<S, R>
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
        private val scan: ScanFun<S, R>
    ) {
        fun create(stateFlow: Flow<S>): Flow<UpdateFun<S>> = flow(stateFlow).onEvent(scan)
    }

    class Updater<S, E>(
        private val flow: Flow<E>,
        private val scan: ScanFun<S, E>
    ) {
        fun create(): Flow<UpdateFun<S>> = flow.onEvent(scan)
    }
}

fun <E, S> Flow<E>.onEvent(update: ScanFun<S, E>): Flow<UpdateFun<S>> =
    onEvent(scanSource(update)) {}

fun <S, E> Flow<E>.onEvent(
    atFirst: UpdaterFlowBuilderScope.SourceElement<S, E>,
    block: UpdaterFlowBuilderScope<S, E>.() -> Unit
): Flow<UpdateFun<S>> {
    val builder = UpdaterFlowBuilderScope(this, atFirst)
    builder.block()
    return builder.build()
}

fun <S, E> scanSource(scan: ScanFun<S, E>): UpdaterFlowBuilderScope.SourceElement<S, E> =
    UpdaterFlowBuilderScope.SourceElement.Scan(listOf(scan))

class UpdaterFlowBuilderScope<S, E>(
    private val eventSource: Flow<E>,
    onEvent: SourceElement<S, E>
) {
    companion object {
        internal fun <S, E> onEvent(scan: ScanFun<S, E>): SourceElement<S, E> =
            SourceElement.Scan(listOf(scan))

        internal fun <S, E, R> onEvent(
            withResult: suspend (S, E) -> Result<R>,
            onSuccess: List<ScanFun<S, R>> = emptyList(),
            onError: List<ScanFun<S, Throwable>> = emptyList(),
        ): SourceElement<S, E> = SourceElement.ToResult(withResult, onSuccess, onError)
    }

    private val tasks = mutableListOf(onEvent)

    fun onNext(scan: ScanFun<S, E>) {
        tasks.add(onEvent(scan))
    }

    fun <R> onNext(
        withResult: suspend (S, E) -> Result<R>,
        onSuccess: List<ScanFun<S, R>> = emptyList(),
        onError: List<ScanFun<S, Throwable>> = emptyList(),
    ) {
        tasks.add(onEvent(withResult, onSuccess, onError))
    }

    internal fun build(): Flow<UpdateFun<S>> = flow {
        eventSource.collect { e ->
            tasks.forEach { it(this, e) }
        }
    }

    sealed class SourceElement<S, E> {
        internal class Scan<S, E>(private val blocks: List<ScanFun<S, E>>) : SourceElement<S, E>() {
            override suspend fun invoke(flowCollector: FlowCollector<UpdateFun<S>>, event: E) {
                blocks.forEach {
                    val mu: UpdateFun<S> = { s -> it(s, event) }
                    flowCollector.emit(mu)
                }
            }
        }

        internal class ToResult<S, E, R>(
            private val result: suspend (S, E) -> Result<R>,
            private val onSuccess: List<ScanFun<S, R>> = emptyList(),
            private val onError: List<ScanFun<S, Throwable>> = emptyList(),
        ) : SourceElement<S, E>() {
            init {
                require(onSuccess.isNotEmpty() || onError.isNotEmpty())
            }

            override suspend fun invoke(flowCollector: FlowCollector<UpdateFun<S>>, event: E) {
                val channel: Channel<Result<R>> = Channel()
                val firstOnSuccess = onSuccess.firstOrNull()
                val firstOnError = onError.firstOrNull()
                val updater: UpdateFun<S> = { s ->
                    val r = result(s, event)
                    val updated = when {
                        r.isSuccess -> firstOnSuccess?.invoke(s, r.getOrThrow())
                        else -> firstOnError?.invoke(s, requireNotNull(r.exceptionOrNull()))
                    } ?: s
                    channel.send(r)
                    updated
                }
                flowCollector.emit(updater)

                val res = channel.receive()
                channel.close()
                if (res.isSuccess) {
                    val afterOnSuccessTasks = onSuccess.drop(1)
                    if (afterOnSuccessTasks.isNotEmpty()) {
                        Scan(afterOnSuccessTasks).invoke(flowCollector, res.getOrThrow())
                    }
                } else if (res.isFailure) {
                    val afterOnErrorTasks = onError.drop(1)
                    if (afterOnErrorTasks.isNotEmpty()) {
                        val exception = requireNotNull(res.exceptionOrNull())
                        Scan(afterOnErrorTasks).invoke(flowCollector, exception)
                    }
                }
            }
        }

        abstract suspend operator fun invoke(flowCollector: FlowCollector<UpdateFun<S>>, event: E)
    }
}
