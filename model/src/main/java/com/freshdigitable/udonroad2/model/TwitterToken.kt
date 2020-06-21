/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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

interface RequestTokenItem : Serializable {
    val token: Serializable
    val authorizationUrl: String
}

interface AccessTokenEntity {
    val userId: Long
    val token: String
    val tokenSecret: String

    companion object {
        fun create(
            userId: Long,
            token: String,
            tokenSecret: String
        ): AccessTokenEntity = AccessTokenEntityImpl(
            userId,
            token,
            tokenSecret
        )
    }
}

private data class AccessTokenEntityImpl(
    override val userId: Long,
    override val token: String,
    override val tokenSecret: String
) : AccessTokenEntity