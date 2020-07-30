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

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.observe
import io.reactivex.disposables.Disposable

@Deprecated(message = "planned replacing to ActivityNavigation class")
abstract class Navigation<T : FragmentContainerState>(
    val navigator: NavigationDispatcher,
    protected val activity: AppCompatActivity
) : LifecycleObserver {

    private val state: MutableLiveData<T?> = MutableLiveData()
    protected val currentState: T?
        get() = state.value

    private val eventDisposable: Disposable

    init {
        activity.lifecycle.addObserver(this)
        eventDisposable = navigator.emitter
            .subscribe { event ->
                val s = onEvent(event) ?: return@subscribe
                state.postValue(s)
            }

        state.observe(activity) { navigate(it) }
    }

    abstract fun onEvent(event: NavigationEvent): T?
    abstract fun navigate(s: T?)

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        eventDisposable.dispose()
    }
}
