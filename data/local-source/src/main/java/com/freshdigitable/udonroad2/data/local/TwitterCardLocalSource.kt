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

package com.freshdigitable.udonroad2.data.local

import com.freshdigitable.udonroad2.data.TwitterCardDataSource
import com.freshdigitable.udonroad2.model.TwitterCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

internal class TwitterCardLocalSource @Inject constructor() : TwitterCardDataSource.Local {
    private val source = MutableStateFlow<Map<String, TwitterCard>>(emptyMap())

    override fun getTwitterCardSource(url: String): Flow<TwitterCard?> =
        source.mapLatest { it[url] }

    override suspend fun putTwitterCard(url: String, card: TwitterCard) {
        val cards = source.value + mapOf(url to card)
        source.value = cards
    }
}
