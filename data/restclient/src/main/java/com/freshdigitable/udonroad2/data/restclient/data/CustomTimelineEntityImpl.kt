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

package com.freshdigitable.udonroad2.data.restclient.data

import com.freshdigitable.udonroad2.model.CustomTimelineEntity
import com.freshdigitable.udonroad2.model.CustomTimelineId
import com.freshdigitable.udonroad2.model.user.UserEntity

internal data class CustomTimelineEntityImpl(
    override val user: UserEntity,
    override val id: CustomTimelineId,
    override val name: String,
    override val description: String,
    override val memberCount: Int,
    override val followerCount: Int,
    override val isPublic: Boolean
) : CustomTimelineEntity