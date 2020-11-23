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

package com.freshdigitable.udonroad2.shortcut

import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.fragment.app.testing.withFragment
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.AttributeSetBuilder

@RunWith(AndroidJUnit4::class)
class TweetDetailContextMenuViewTest {
    @Test
    fun init() {
        // setup
        val sut = launchContainerFragment()
            .moveToState(Lifecycle.State.RESUMED)
            .withFragment { sut }

        // verify
        assertThat(sut).isNotNull()
        assertThat(sut.parent).isNotNull()
        assertThat(sut.y).isEqualTo((sut.parent as View).height - sut.height)
    }

    @Test
    fun initWithMenuMain() {
        // setup
        val sut = launchContainerFragment {
            addAttribute(R.attr.menu_main, "@menu/detail_main")
        }
            .moveToState(Lifecycle.State.RESUMED)
            .withFragment { sut }

        // verify
        assertThat(sut).isNotNull()
        assertThat(sut.parent).isNotNull()
        assertThat(sut.y).isEqualTo((sut.parent as View).height - sut.height)
        assertThat(sut.findViewById<ViewGroup>(R.id.detail_menu_main).childCount).isEqualTo(5)
    }
}

internal fun launchContainerFragment(
    initialState: Lifecycle.State = Lifecycle.State.CREATED,
    additionalAttrs: (AttributeSetBuilder.() -> Unit)? = null
): FragmentScenario<ContainerFragment> {
    return launchFragmentInContainer<ContainerFragment>(
        initialState = initialState,
        factory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                if (className == ContainerFragment::class.java.name) {
                    val attrs = additionalAttrs?.let {
                        Robolectric.buildAttributeSet().apply(it).build()
                    }
                    return ContainerFragment(attrs)
                }
                return super.instantiate(classLoader, className)
            }
        }
    )
}

internal class ContainerFragment(
    private val attrs: AttributeSet? = null
) : Fragment() {
    lateinit var sut: TweetDetailContextMenuView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FrameLayout(requireContext()).apply {
        addView(
            TweetDetailContextMenuView(requireContext(), attrs),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sut = (view as FrameLayout).getChildAt(0) as TweetDetailContextMenuView
    }
}
