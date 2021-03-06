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

package com.freshdigitable.udonroad2.model.user

import com.freshdigitable.udonroad2.model.UserId

interface Relationship {
    val targetUserId: UserId
    val following: Boolean
    val blocking: Boolean
    val muting: Boolean
    val wantRetweets: Boolean
    val notificationsEnabled: Boolean
    val sourceUserId: UserId

    companion object
}
