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

package com.freshdigitable.udonroad2.input

import android.view.Menu

enum class InputMenuItem(
    val itemId: Int,
    val enabled: Boolean
) {
    WRITE_ENABLED(R.id.input_tweet_write, true),
    WRITE_DISABLED(R.id.input_tweet_write, false),
    SEND_ENABLED(R.id.input_tweet_send, true),
    SEND_DISABLED(R.id.input_tweet_send, false),
    RETRY_ENABLED(R.id.input_tweet_error, true),
}

fun Menu.prepareItem(available: InputMenuItem) {
    InputMenuItem.values().map { it.itemId }.distinct().forEach {
        val item = findItem(it)
        when (item.itemId) {
            available.itemId -> {
                item.isVisible = true
                item.isEnabled = available.enabled
            }
            else -> {
                item.isVisible = false
            }
        }
    }
}
