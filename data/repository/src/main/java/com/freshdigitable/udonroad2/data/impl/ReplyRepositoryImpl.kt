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

package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.ReplyRepository
import com.freshdigitable.udonroad2.data.db.LocalSourceModule
import dagger.Binds
import dagger.Module
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReplyRepositoryImpl @Inject constructor(
    private val localSource: ReplyRepository.LocalSource
) : ReplyRepository by localSource

@Module(includes = [LocalSourceModule::class])
interface ReplyRepositoryModule {
    @Binds
    fun bindReplyRepository(repository: ReplyRepositoryImpl): ReplyRepository
}