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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.mainContext
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.coroutines.CoroutineContext

fun <T> Flow<T>.testCollect(executor: AppExecutor): List<T> =
    testCollect(executor, executor.mainContext)

fun <T> Flow<T>.testCollect(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext = coroutineScope.coroutineContext
): List<T> {
    val actual = mutableListOf<T>()
    coroutineScope.launch(coroutineContext) {
        collect { actual.add(it) }
    }
    return actual
}

class ObserverEventCollector(
    private val coroutineRule: CoroutineTestRule = CoroutineTestRule(),
) : TestWatcher() {
    private val observers = mutableMapOf<LiveData<*>, ObserverCollector>()
    fun addAll(vararg liveData: LiveData<*>) {
        liveData.forEach { d ->
            val observer = ObserverCollector()
            observers[d] = observer
        }
    }

    fun <T> eventsOf(liveData: LiveData<T>): List<T?> =
        requireNotNull(observers[liveData]).events as List<T?>

    private val scope =
        CoroutineScope(coroutineRule.coroutineContextProvider.mainContext + SupervisorJob())
    private val collectors = mutableMapOf<Flow<*>, ObserverCollector>()
    fun addAll(vararg flow: Flow<*>) {
        flow.forEach { f ->
            val collector = ObserverCollector()
            collectors[f] = collector
        }
    }

    fun addActivityEventStream(stream: ActivityEventStream) {
        addAll(stream.feedbackMessage, stream.navigationEvent)
    }

    fun <T> eventsOf(flow: Flow<T>): List<T?> = requireNotNull(collectors[flow]).events as List<T?>
    inline fun <reified T> nonNullEventsOf(flow: Flow<T>): List<T> = eventsOf(flow).map { it as T }

    fun activateAll() {
        observers.forEach { (l, c) -> l.observeForever(c) }
        collectors.forEach { (f, c) ->
            scope.launch { f.collect { c(it) } }
        }
    }

    fun deactivateAll() {
        coroutineRule.runBlockingTest {
            observers.forEach { (l, c) -> l.removeObserver(c) }
        }
        scope.cancel()
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        val apply = super.apply(base, description)
        return coroutineRule.apply(apply, description)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        scope.cancel()
    }

    class ObserverCollector : Observer<Any> {
        private val _events = mutableListOf<Any?>()
        val events: List<Any?> = _events

        override fun onChanged(t: Any?) {
            _events.add(t)
        }

        operator fun invoke(event: Any?) {
            _events.add(event)
        }
    }
}

fun ObserverEventCollector.setupForActivate(block: ObserverEventCollector.() -> Unit) {
    block()
    activateAll()
}

inline fun <reified T : NavigationEvent> ObserverEventCollector.assertLatestNavigationEvent(
    flow: Flow<NavigationEvent>,
    assertBlock: (T) -> Unit,
) {
    assertNavigationEvent(flow, { it.last() }, assertBlock)
}

inline fun <reified T : NavigationEvent> ObserverEventCollector.assertNavigationEvent(
    flow: Flow<NavigationEvent>,
    eventPicker: (List<NavigationEvent>) -> NavigationEvent,
    assertBlock: (T) -> Unit,
) {
    val events = nonNullEventsOf(flow)
    val actual = eventPicker(events)
    assertThat(actual).isInstanceOf(T::class.java)
    assertBlock(actual as T)
}
