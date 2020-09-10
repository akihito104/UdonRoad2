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

package com.freshdigitable.udonroad2.user

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.user.UserId
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UserViewModelTest {
    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val userRepository = mockk<UserRepository>()
    private val relationshipRepository = mockk<RelationshipRepository>()
    private val sut = UserViewModel(userRepository, relationshipRepository)

    @Before
    fun setup() {
        sut.user.observeForever { }
        sut.relationship.observeForever { }
        sut.fabVisible.observeForever { }
        sut.titleAlpha.observeForever { }
    }

    @Test
    fun initialValue() {
        // verify
        assertThat(sut.user.value).isNull()
        assertThat(sut.relationship.value).isNull()
        assertThat(sut.fabVisible.value).isFalse()
        assertThat(sut.titleAlpha.value).isEqualTo(0)
    }

    @Test
    fun setUserId() {
        // setup
        val targetUser = UserId(1000)
        every { userRepository.getUser(targetUser) } returns MutableLiveData(mockk())
        every {
            relationshipRepository.findRelationship(targetUser)
        } returns MutableLiveData(mockk())

        // exercise
        sut.setUserId(targetUser)

        // verify
        verify { userRepository.getUser(targetUser) }
        verify { relationshipRepository.findRelationship(targetUser) }
        assertThat(sut.user.value).isNotNull()
        assertThat(sut.relationship.value).isNotNull()
    }

    @Test
    fun setAppbarScrollRate() {
        // exercise
        sut.setAppBarScrollRate(1f)

        // verify
        assertThat(sut.titleAlpha.value).isEqualTo(1f)
    }
}
