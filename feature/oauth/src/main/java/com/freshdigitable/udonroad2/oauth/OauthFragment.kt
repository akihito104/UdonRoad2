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

package com.freshdigitable.udonroad2.oauth

import android.os.Bundle
import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.FragmentScope
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector
import kotlin.reflect.KClass

class OauthFragment : ListItemFragment<OauthViewModel, QueryType.Oauth, OauthItem>() {
    override val viewModelClass: KClass<OauthViewModel> = OauthViewModel::class
    private var viewModel: OauthViewModel? = null

    override fun createListAdapter(viewModel: OauthViewModel): PagedListAdapter<OauthItem, *> {
        this.viewModel = viewModel
        return OauthListAdapter(viewModel, viewLifecycleOwner)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel?.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewModel?.onViewStateRestore(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel = null
    }
}

@Module
interface OauthFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributeOauthFragment(): OauthFragment
}
