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

package com.freshdigitable.udonroad2.shortcut_actions

import androidx.annotation.StringRes
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage

internal enum class TweetFeedbackMessage(
    @StringRes override val messageRes: Int
) : FeedbackMessage {
    FAV_CREATE_SUCCESS(R.string.msg_fav_create_success),
    FAV_CREATE_FAILURE(R.string.msg_fav_create_failure),
    ALREADY_FAV(R.string.msg_already_fav),

    RT_CREATE_SUCCESS(R.string.msg_rt_create_success),
    RT_CREATE_FAILURE(R.string.msg_rt_create_failure),
    ALREADY_RT(R.string.msg_already_rt),
}
