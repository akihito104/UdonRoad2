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

package com.freshdigitable.udonroad2.oauth

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDelegate
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.weakRef
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentEventDelegate

internal class OauthNavigationDelegate(
    listItemFragment: ListItemFragment,
    private val activityEventDelegate: ActivityEventDelegate,
) : NavigationDelegate, ListItemFragmentEventDelegate {
    private val activity: FragmentActivity by weakRef(listItemFragment) { it.requireActivity() }

    override fun dispatchNavHostNavigate(event: NavigationEvent) {
        when (event) {
            is OauthEvent.Navigation.LaunchTwitter -> launchTwitterOauth(event.url)
            else -> activityEventDelegate.dispatchNavHostNavigate(event)
        }
    }

    override fun dispatchFeedbackMessage(message: FeedbackMessage) {
        activityEventDelegate.dispatchFeedbackMessage(message)
    }

    private fun launchTwitterOauth(authUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        activity.startActivity(intent)
    }
}
