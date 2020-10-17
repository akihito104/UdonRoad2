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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.data.impl.TweetInputRepository
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.ext.merge
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

class TweetInputViewModel @Inject constructor(
    collapsible: Boolean,
    private val eventDispatcher: EventDispatcher,
    private val repository: TweetInputRepository,
    private val executor: AppExecutor,
) : ViewModel() {

    private val _state = MutableLiveData(
        if (collapsible) TweetInputState.IDLING else TweetInputState.OPENED
    )
    val isVisible: LiveData<Boolean> = _state.map {
        when (it) {
            TweetInputState.OPENED -> true
            else -> false
        }
    }

    val text: LiveData<String> = repository.text.asLiveData(executor.dispatcher.mainContext)
    val menuItem: LiveData<InputMenuItem> = merge(_state, text) { s, t ->
        when (s) {
            null -> throw IllegalStateException()
            TweetInputState.OPENED -> {
                if (t.isNullOrBlank()) {
                    InputMenuItem.SEND_DISABLED
                } else {
                    InputMenuItem.SEND_ENABLED
                }
            }
            else -> s.toMenuItem()
        }
    }

    private val disposable = CompositeDisposable(
        eventDispatcher.toAction<TweetInputEvent.Open>().subscribe {
            _state.value = TweetInputState.OPENED
        },
        eventDispatcher.toAction<TweetInputEvent.Send>().subscribe {
            postTweet()
        },
        eventDispatcher.toAction<TweetInputEvent.Close>().subscribe {
            _state.value = TweetInputState.IDLING
            repository.clear()
        }
    )

    fun onWriteClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Open)
    }

    fun onTweetTextChanged(text: String) {
        repository.updateText(text)
    }

    fun onSendClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Send)
    }

    private fun postTweet() {
        _state.value = TweetInputState.SENDING
        viewModelScope.launch(executor.dispatcher.mainContext) {
            try {
                repository.post()
                _state.value = TweetInputState.SUCCEEDED
                repository.clear()
                _state.value = TweetInputState.IDLING
            } catch (e: IOException) {
                _state.value = TweetInputState.FAILED
            }
        }
    }

    fun onCloseClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Close)
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}
