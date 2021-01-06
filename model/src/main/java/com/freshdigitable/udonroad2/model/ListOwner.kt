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

package com.freshdigitable.udonroad2.model

import java.io.Serializable

data class ListOwner<Q : QueryType>(
    val id: ListId,
    val query: Q
) : Serializable {
    constructor(id: Int, query: Q) : this(ListId(id), query)

    @Deprecated("use ListOwner.id")
    val value: String = "${id.value}"
}

interface ListEntity {
    companion object {
        const val CURSOR_INIT: Long = -1
        val CURSOR_REACH_TO_END: Long? = null

        val ListEntity.hasNotFetchedYet: Boolean
            get() = prependCursor == CURSOR_INIT && appendCursor == CURSOR_INIT
        val ListEntity.isReachedToEnd: Boolean
            get() = appendCursor == CURSOR_REACH_TO_END
    }

    val id: ListId

    /**
     * to get list items that will be loaded upper side of current list.
     */
    val prependCursor: Long

    /**
     * to get list items that will be loaded bottom side of current list.
     */
    val appendCursor: Long?
}

data class ListId(val value: Int) : Serializable

interface ListOwnerGenerator {
    suspend fun <Q : QueryType> generate(type: Q): ListOwner<Q>

    companion object
}
