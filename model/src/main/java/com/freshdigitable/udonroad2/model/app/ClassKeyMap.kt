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

typealias ClassKeyMap<K, V> = Map<Class<out K>, @JvmSuppressWildcards V>

fun <K : Any, V> ClassKeyMap<K, V>.valueByAssignableClassObject(key: K): V {
    val clz = key::class.java
    return valueByAssignableClass(clz)
}

fun <K : Any, V> ClassKeyMap<K, V>.valueByAssignableClass(key: Class<out K>): V {
    return this[key] ?: this.toList().firstOrNull { (c, _) ->
        c.isAssignableFrom(key)
    }?.second ?: throw IllegalStateException(
        "unregistered: ${key.name}, all classes: ${this.keys.map { it.name }}"
    )
}
