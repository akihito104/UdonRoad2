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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.addTo
import com.freshdigitable.udonroad2.model.app.weakRef
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class OauthNavigationDelegate @Inject constructor(
    listItemFragment: ListItemFragment,
) : LifecycleEventObserver {
    private val activity: FragmentActivity by weakRef(listItemFragment) { it.requireActivity() }
    private val lifecycleOwner: LifecycleOwner by weakRef(listItemFragment)
    private val disposables = CompositeDisposable()

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    fun <T> subscribeWith(action: AppAction<T>, block: OauthNavigationDelegate.(T) -> Unit) {
        action.subscribe { block(it) }.addTo(disposables)
    }

    internal fun launchTwitterOauth(authUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        activity.startActivity(intent)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            disposables.clear()
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    }
}
