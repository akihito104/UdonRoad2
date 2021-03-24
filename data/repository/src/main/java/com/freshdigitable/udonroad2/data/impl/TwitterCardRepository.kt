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

package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.TwitterCardDataSource
import com.freshdigitable.udonroad2.model.TwitterCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class TwitterCardRepository @Inject constructor(
    private val localDataSource: TwitterCardDataSource.Local,
    private val remoteSource: TwitterCardDataSource.Remote
) : TwitterCardDataSource by localDataSource {

    override fun getTwitterCardSource(url: String): Flow<TwitterCard?> = flow {
        localDataSource.getTwitterCardSource(url).collect { card ->
            if (card != null) {
                emit(card)
            } else {
                emitAll(remoteSource.getTwitterCardSource(url).onEach { c ->
                    c?.let { localDataSource.putTwitterCard(url, it) }
                })
            }
        }
    }
}
