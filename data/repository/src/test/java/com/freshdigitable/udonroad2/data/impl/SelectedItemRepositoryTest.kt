/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.ObserverEventCollector
import com.freshdigitable.udonroad2.test_common.jvm.setupForActivate
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class SelectedItemRepositoryTest {
    companion object {
        private val owners = listOf<ListOwner<*>>(
            ListOwner(ListId(0), QueryType.TweetQueryType.Timeline()),
            ListOwner(ListId(1), QueryType.TweetQueryType.Fav()),
        )
        private val tweets = listOf(TweetId(1000), TweetId(2000), TweetId(3000))
        val timeline = mapOf(
            owners[0] to listOf(tweets[0], tweets[1]),
            owners[1] to listOf(tweets[0], tweets[2]),
        )

        fun ListOwner<*>.selectAt(index: Int): SelectedItemId =
            SelectedItemId(this, requireNotNull(timeline[this]).get(index))
    }

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val sut = SelectedItemRepository()

    @Test
    fun put() {
        // setup
        val selected = owners[0].selectAt(0)

        // exercise
        sut.put(selected)

        // verify
        assertThat(sut.find(owners[0])).isEqualTo(selected)
    }

    @Test
    fun put_overwrite() {
        // setup
        val target = owners[0].selectAt(1)

        // exercise
        sut.put(owners[0].selectAt(0))
        sut.put(target)

        // verify
        assertThat(sut.find(owners[0])).isEqualTo(target)
    }

    @Test
    fun remove() {
        // exercise
        sut.put(owners[0].selectAt(0))
        sut.remove(owners[0])

        // verify
        assertThat(sut.find(owners[0])).isNull()
    }

    @Test
    fun remove_forUnregisteredItem_then_noUpdateIsOccurred() {
        // exercise
        sut.put(owners[0].selectAt(0))
        sut.remove(owners[1])

        // verify
        assertThat(sut.find(owners[0])).isEqualTo(owners[0].selectAt(0))
    }

    @Test
    fun getSource_putSimply() {
        // setup
        val selected = owners[0].selectAt(0)
        val actualItems = ObserverEventCollector(coroutineRule).run {
            val source = sut.getSource(owners[0])
            setupForActivate { addAll(source) }
            eventsOf(source)
        }

        // exercise
        coroutineRule.runBlockingTest {
            sut.put(selected)
        }

        // verify
        assertThat(actualItems.last()).isEqualTo(selected)
    }

    @Test
    fun getSource_putAndRemove() {
        // setup
        val selected = owners[0].selectAt(0)
        val actualItems = ObserverEventCollector(coroutineRule).run {
            val source = sut.getSource(owners[0])
            setupForActivate { addAll(source) }
            eventsOf(source)
        }

        // exercise
        coroutineRule.runBlockingTest {
            sut.put(selected)
            sut.remove(owners[0])
        }

        // verify
        assertThat(actualItems).hasSize(3)
        assertThat(actualItems.last()).isNull()
    }

    @Test
    fun getSource_putToOverwrite() {
        // setup
        val actualItems = ObserverEventCollector(coroutineRule).run {
            val source = sut.getSource(owners[0])
            setupForActivate { addAll(source) }
            eventsOf(source)
        }

        // exercise
        coroutineRule.runBlockingTest {
            sut.put(owners[0].selectAt(0))
            sut.put(owners[0].selectAt(1))
        }

        // verify
        assertThat(actualItems).hasSize(3)
        assertThat(actualItems.last()).isEqualTo(owners[0].selectAt(1))
    }

    @Test
    fun getSource_putOtherOwner() {
        // setup
        val actualItems = ObserverEventCollector(coroutineRule).run {
            val source = sut.getSource(owners[0])
            setupForActivate { addAll(source) }
            eventsOf(source)
        }

        // exercise
        coroutineRule.runBlockingTest {
            sut.put(owners[0].selectAt(0))
            sut.put(owners[1].selectAt(0))
        }

        // verify
        assertThat(actualItems).hasSize(2)
        assertThat(actualItems.last()).isEqualTo(owners[0].selectAt(0))
    }

    @Test
    fun getSource_removeUnregisteredItem_then_noUpdateIsOccurred() {
        // setup
        val actualItems = ObserverEventCollector(coroutineRule).run {
            val source = sut.getSource(owners[0])
            setupForActivate { addAll(source) }
            eventsOf(source)
        }

        // exercise
        coroutineRule.runBlockingTest {
            sut.put(owners[0].selectAt(0))
            sut.remove(owners[1])
        }

        // verify
        assertThat(actualItems).hasSize(2)
        assertThat(actualItems.last()).isEqualTo(owners[0].selectAt(0))
    }
}
