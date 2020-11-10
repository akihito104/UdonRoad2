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

import kotlin.reflect.KClass

class StateGraph<S : Any, E : Any>(
    private val graphs: List<Graph<S, E>>
) {
    fun transition(s: S, e: E): S {
        @Suppress("UNCHECKED_CAST")
        return graphs.first { it.matches(s, e) }.transition(s, e) as S
    }

    data class Graph<S : Any, E : Any>(
        val state: GraphElement<S>,
        val event: GraphElement<E>,
        val transition: S.(E) -> Any
    ) {
        fun matches(s: S, e: E): Boolean = state.matches(s) && event.matches(e)
    }

    sealed class GraphElement<T> {
        class KClassGraphElement<T : Any>(private val elem: KClass<T>) : GraphElement<T>() {
            override fun matches(t: T): Boolean = elem == t::class
        }

        class EnumGraphElement<T : Any>(private val elem: T) : GraphElement<T>() {
            override fun matches(t: T): Boolean = elem == t
        }

        abstract fun matches(t: T): Boolean
    }

    companion object {
        inline fun <reified S : Any, E : Any> create(
            block: Builder<S, E>.() -> Unit
        ): StateGraph<S, E> {
            val builder = Builder<S, E>(S::class)
            builder.block()
            @Suppress("UNCHECKED_CAST")
            return StateGraph(builder.gs as List<Graph<S, E>>)
        }
    }

    class Builder<S : Any, E : Any>(
        val pS: KClass<S>,
    ) {
        val gs: MutableList<Graph<out S, out E>> = mutableListOf()

        inline fun <reified SS : S> state(b: StateScope<S, SS, E>.() -> Unit) {
            val s = StateScope<S, SS, E>(GraphElement.KClassGraphElement(SS::class), pS)
            s.b()
            gs.addAll(s.gs)
        }

        fun state(ss: S, b: StateScope<S, S, E>.() -> Unit) {
            val s = StateScope<S, S, E>(GraphElement.EnumGraphElement(ss), pS)
            s.b()
            gs.addAll(s.gs)
        }

        class StateScope<S : Any, SS : S, E : Any>(
            val ss: GraphElement<SS>,
            val pS: KClass<S>,
        ) {
            val gs = mutableListOf<Graph<out S, out E>>()

            inline fun <reified EE : E> accept(noinline b: SS.(EE) -> S) {
                gs.add(Graph(ss, GraphElement.KClassGraphElement(EE::class), b))
            }

            inline fun <reified EE : E> doNotCare() {
                gs.add(Graph(ss, GraphElement.KClassGraphElement(EE::class)) { this })
            }
        }
    }
}
