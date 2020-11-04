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

package com.freshdigitable.udonroad2.model.app.ext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <I1, I2, O> combineLatest(
    d1: LiveData<I1>,
    d2: LiveData<I2>,
    block: LiveData<O>.(I1?, I2?) -> O
): LiveData<O> {
    val res = MediatorLiveData<O>()
    res.addSource(d1) { d ->
        res.value = block(res, d, d2.value)
    }
    res.addSource(d2) { d ->
        res.value = block(res, d1.value, d)
    }
    return res
}

fun <T> LiveData<T?>.filterNotNull(): LiveData<T> {
    val res = MediatorLiveData<T>()
    res.addSource(this) {
        if (it != null) {
            res.value = it
        }
    }
    return res
}
