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

package com.freshdigitable.udonroad2.model.app

import java.lang.ref.WeakReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T : Any> weakRef(t: T): ReadOnlyProperty<Any, T> = WeakRefProperty(t)

private class WeakRefProperty<T : Any>(t: T) : ReadOnlyProperty<Any, T> {
    private val _t: WeakReference<T> = WeakReference(t)
    override fun getValue(thisRef: Any, property: KProperty<*>): T = requireNotNull(_t.get())
}

fun <T : Any, R : Any> weakRef(
    t: T,
    lazyBlock: (T) -> R
): ReadOnlyProperty<Any, R> = WeakRefPropertyWithLazy(t, lazyBlock)

private class WeakRefPropertyWithLazy<R : Any, T : Any>(
    r: R,
    lazyBlock: (R) -> T
) : ReadOnlyProperty<Any, T> {
    val rRef: R by weakRef(r)
    val tRef: WeakReference<T> by lazy { WeakReference(lazyBlock(rRef)) }
    override fun getValue(thisRef: Any, property: KProperty<*>): T = requireNotNull(tRef.get())
}
