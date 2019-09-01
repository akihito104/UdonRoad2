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

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.viewmodel.ListOwner
import dagger.Module
import dagger.Provides

class OauthViewModel(
    dataSource: OauthDataSource
) : ViewModel(), ListItemLoadable<OauthItem> {

    override val loading: LiveData<Boolean> = MutableLiveData<Boolean>(false)
    private val livePagedList = MutableLiveData(PagedList.Builder(dataSource, 10).build())

    override fun getList(listOwner: ListOwner): LiveData<PagedList<OauthItem>> = livePagedList

    override fun onRefresh() {}
}

@Module
interface OauthViewModelModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideOauthDataSource(context: Context): OauthDataSource {
            return OauthDataSource(context)
        }

        @JvmStatic
        @Provides
        fun provideOauthViewModel(dataSource: OauthDataSource): OauthViewModel {
            return OauthViewModel(dataSource)
        }
    }
}
