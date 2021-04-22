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

package com.freshdigitable.fabshortcut

import android.os.Bundle
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.core.view.isInvisible
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.AttributeSetBuilder

@RunWith(AndroidJUnit4::class)
class ExpandableBottomContextMenuViewTest {
    @Test
    fun init() {
        // setup
        val sut = launchContainerFragment()
            .moveToState(Lifecycle.State.RESUMED)
            .withFragment { sut }

        // verify
        assertThat(sut.parent).isNotNull()
        assertThat(sut.mainList.childCount).isEqualTo(0)
        assertThat(sut.moreList.childCount).isEqualTo(0)
        checkPeekHeight(sut)
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
        assertThat(sut.parent).isNotNull()
        assertThat(sut.toggle).isNull()
        assertThat(sut.mainList.childCount).isEqualTo(5)
        assertThat(sut.moreList.childCount).isEqualTo(0)
        checkPeekHeight(sut)
        assertThat(sut.mainListChildByMenuItem(R.id.detail_main_rt).drawableState).asList()
            .contains(android.R.attr.state_checkable)
    }

    @Test
    fun initWithMenuMainAndMore() {
        // setup
        val sut = launchContainerFragment {
            addAttribute(R.attr.menu_main, "@menu/detail_main")
            addAttribute(R.attr.menu_more, "@menu/detail_more")
        }
            .moveToState(Lifecycle.State.RESUMED)
            .withFragment { sut }

        // verify
        assertThat(sut.parent).isNotNull()
        assertThat(sut.toggle).isNotNull()
        assertThat(sut.toggle).isEqualTo(sut.mainList.children.maxByOrNull { it.x })
        assertThat(sut.mainList.children.count { it is ImageButton }).isEqualTo(6)
        assertThat(sut.moreList.childCount).isEqualTo(1)
        checkPeekHeight(sut)
    }

    @Test
    fun updateMenuItem_whenCheckableItemIsChecked_then_viewStateIsUpdated() {
        // setup
        val sut = launchContainerFragment {
            addAttribute(R.attr.menu_main, "@menu/detail_main")
        }.moveToState(Lifecycle.State.RESUMED)
            .withFragment {
                // exercise
                sut.updateMenuItem {
                    onMenuItem(R.id.detail_main_rt) {
                        isChecked = true
                    }
                }
                sut
            }

        // verify
        assertThat(sut.parent).isNotNull()
        assertThat(sut.mainListChildByMenuItem(R.id.detail_main_rt).drawableState).asList()
            .containsAtLeast(android.R.attr.state_checked, android.R.attr.state_checkable)
    }

    @Test
    fun updateMenuItem_changeMoreItemToZero_then_toggleIsInvisible() {
        // setup
        val sut = launchContainerFragment {
            addAttribute(R.attr.menu_main, "@menu/detail_main")
            addAttribute(R.attr.menu_more, "@menu/detail_more")
        }.moveToState(Lifecycle.State.RESUMED)
            .withFragment {
                // exercise
                sut.updateMenuItem {
                    onMenuItem(R.id.detail_more_delete) {
                        isVisible = false
                    }
                }
                shadowOf(Looper.getMainLooper()).idle()
                sut
            }

        // verify
        assertThat(sut.parent).isNotNull()
        assertThat(sut.toggle?.isInvisible).isTrue()
        assertThat(sut.moreList.childCount).isEqualTo(0)
    }

    @Test
    fun updateMenuItem_changeGroupEnabled_then_applyForGroup() {
        // setup
        val sut = launchContainerFragment {
            addAttribute(R.attr.menu_main, "@menu/detail_main")
            addAttribute(R.attr.menu_more, "@menu/detail_more")
        }.moveToState(Lifecycle.State.RESUMED)
            .withFragment {
                // exercise
                sut.updateMenuItem {
                    changeGroupEnabled(R.id.menuGroup_detailMain, true)
                }
                shadowOf(Looper.getMainLooper()).idle()
                sut
            }

        // verify
        assertThat(sut.parent).isNotNull()
        assertThat(sut.mainListChildByMenuItem(R.id.detail_main_rt).drawableState).asList()
            .contains(android.R.attr.state_enabled)
        assertThat(sut.mainListChildByMenuItem(R.id.detail_main_fav).drawableState).asList()
            .contains(android.R.attr.state_enabled)
    }

    private fun checkPeekHeight(sut: ExpandableBottomContextMenuView) {
        assertThat(sut.y).isEqualTo((sut.parent as View).height - sut.mainList.height)
    }
}

internal fun launchContainerFragment(
    initialState: Lifecycle.State = Lifecycle.State.CREATED,
    additionalAttrs: (AttributeSetBuilder.() -> Unit)? = null,
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

private val ExpandableBottomContextMenuView.mainList: ViewGroup
    get() = findViewById(R.id.bottom_menu_main)
private val ExpandableBottomContextMenuView.moreList: ViewGroup
    get() = findViewById(R.id.bottom_menu_more)
private val ExpandableBottomContextMenuView.toggle: ImageButton?
    get() = findViewById(R.id.expandable_bottom_main_toggle)

private fun ExpandableBottomContextMenuView.mainListChildByMenuItem(itemId: Int): ImageButton {
    return mainList.children.first { it.id == itemId } as ImageButton
}

internal class ContainerFragment(
    private val attrs: AttributeSet? = null,
) : Fragment() {
    lateinit var sut: ExpandableBottomContextMenuView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = CoordinatorLayout(requireContext()).apply {
        addView(
            FrameLayout(requireContext()),
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        addView(
            ExpandableBottomContextMenuView(requireContext(), attrs),
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sut = (view as ViewGroup).getChildAt(1) as ExpandableBottomContextMenuView
    }
}
