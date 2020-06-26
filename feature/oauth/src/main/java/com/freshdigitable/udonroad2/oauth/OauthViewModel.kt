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

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import androidx.paging.DataSource
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlinx.coroutines.launch

class OauthViewModel(
    dataSource: DataSource<Int, OauthItem>,
    private val repository: OAuthTokenRepository,
    private val navigator: NavigationDispatcher
) : ViewModel(), ListItemLoadable<QueryType.Oauth, OauthItem> {

    override val loading: LiveData<Boolean> = MutableLiveData(false)
    private val livePagedList: MutableLiveData<PagedList<OauthItem>>

    init {
        val config = PagedList.Config.Builder()
            .setMaxSize(100)
            .setPageSize(10)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(false)
            .setInitialLoadSizeHint(10)
            .build()
        livePagedList = MutableLiveData(
            PagedList.Builder(dataSource, config)
                .setNotifyExecutor {}
                .setFetchExecutor {}
                .build()
        )
    }

    override val timeline: LiveData<PagedList<OauthItem>> = livePagedList

    override fun onRefresh() {}

    private val requestToken: MutableLiveData<RequestTokenItem?> = MutableLiveData()

    fun onLoginClicked() {
        viewModelScope.launch {
            repository.getRequestTokenItem().also {
                navigator.postEvent(OauthEvent.OauthRequested(it.authorizationUrl))
                requestToken.value = it
            }
        }
    }

    private val _pin = MutableLiveData("")
    val pin: LiveData<String> = _pin.distinctUntilChanged()

    val sendPinButtonEnabled: LiveData<Boolean> = merge(requestToken, pin) { token, p ->
        token != null && !p.isNullOrEmpty()
    }

    fun onAfterPinTextChanged(pin: CharSequence) {
        _pin.value = pin.toString()
    }

    fun onSendPinClicked() {
        viewModelScope.launch {
            val t = repository.getAccessToken(requestToken.value!!, pin.value.toString())
            repository.login(t.userId)
            navigator.postEvent(OauthEvent.OauthSucceeded)
            requestToken.value = null
        }
    }

    internal fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(SAVED_STATE_REQUEST_TOKEN, requestToken.value)
    }

    internal fun onViewStateRestore(savedInstanceState: Bundle?) {
        (savedInstanceState?.getSerializable(SAVED_STATE_REQUEST_TOKEN) as? RequestTokenItem)?.let {
            this.requestToken.value = it
        }
    }

    companion object {
        private const val SAVED_STATE_REQUEST_TOKEN = "saveState_requestToken"
    }
}

sealed class OauthEvent : NavigationEvent {
    object Init : OauthEvent()
    data class OauthRequested(val authUrl: String) : OauthEvent()
    object OauthSucceeded : OauthEvent()
}

@Module
interface OauthViewModelModule {
    companion object {
        @Provides
        fun provideOauthDataSource(context: Application): DataSource<Int, OauthItem> {
            return OauthDataSource(context)
        }

        @Provides
        fun provideOauthViewModel(
            dataSource: DataSource<Int, OauthItem>,
            oAuthTokenRepository: OAuthTokenRepository,
            navigator: NavigationDispatcher
        ): OauthViewModel {
            return OauthViewModel(dataSource, oAuthTokenRepository, navigator)
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(OauthViewModel::class)
    fun bindOauthViewModel(viewModel: OauthViewModel): ViewModel
}

fun <T1, T2, E> merge(t1: LiveData<T1>, t2: LiveData<T2>, block: (T1?, T2?) -> E?): LiveData<E> {
    val res = MediatorLiveData<E>()
    res.addSource(t1) { res.value = block(it, t2.value) }
    res.addSource(t2) { res.value = block(t1.value, it) }
    return res
}
