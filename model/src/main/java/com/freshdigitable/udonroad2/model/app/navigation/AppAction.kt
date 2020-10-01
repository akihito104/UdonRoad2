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

package com.freshdigitable.udonroad2.model.app.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.toLiveData
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

typealias AppAction<T> = Observable<T>
typealias AppViewState<T> = LiveData<T>

inline fun <T, reified E> AppAction<T>.toViewState(): AppViewState<E> {
    val subject = BehaviorSubject.create<E>()
    this.cast(E::class.java).subscribe(subject)
    return subject.toFlowable(BackpressureStrategy.BUFFER)
        .toLiveData()
        .distinctUntilChanged()
}

inline fun <reified T> AppAction<out AppEvent>.filterByType(): AppAction<T> {
    return this.filter { it is T }.cast(T::class.java)
}

@ExperimentalCoroutinesApi
inline fun <E : AppEvent, R> AppAction<E>.suspendMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (E) -> R
): AppAction<EventResult<E, R>> = flatMap { event ->
    rxObservable(coroutineContext) {
        val result = kotlin.runCatching { block(event) }
        channel.send(EventResult(event, result))
    }
}
