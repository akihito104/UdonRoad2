package com.freshdigitable.udonroad2.timeline.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectDelegate
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.databinding.FragmentTimelineBinding
import com.freshdigitable.udonroad2.timeline.di.ListItemAdapterComponent
import com.freshdigitable.udonroad2.timeline.di.ListItemFragmentEffectDelegateComponent
import com.freshdigitable.udonroad2.timeline.di.ListItemViewModelComponent
import com.freshdigitable.udonroad2.timeline.di.viewModel
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ListItemFragment : Fragment() {
    @Inject
    lateinit var listItemViewModelBuilder: ListItemViewModelComponent.Builder

    @Inject
    lateinit var listItemAdapterFactory: ListItemAdapterComponent.Factory

    @Inject
    lateinit var effectDelegate: ListItemFragmentEffectDelegateComponent.Factory
    private lateinit var viewModel: ListItemLoadableViewModel<*>

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentTimelineBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DataBindingUtil.findBinding<FragmentTimelineBinding>(view) ?: return
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = listItemViewModelBuilder
            .owner(listOwner)
            .firstArgs(savedInstanceState)
            .viewModelStoreOwner(activity as AppCompatActivity)
            .build()
            .viewModel("_$ownerId")
        binding.viewModel = viewModel

        val adapter = listItemAdapterFactory.create(viewModel as ViewModel, viewLifecycleOwner)
            .adapter as PagingDataAdapter<Any, *>
        binding.mainList.setup(adapter, viewModel)

        val swipeRefresh = binding.mainSwipeRefresh
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            merge(
                adapter.loadStateFlow.mapLatest {
                    it.refresh is LoadState.Loading ||
                        it.append is LoadState.Loading ||
                        it.prepend is LoadState.Loading
                }.distinctUntilChanged(),
                viewModel.listState.map { it.isPrepending }.asFlow().distinctUntilChanged(),
            ).collectLatest {
                Timber.tag("ListItemFragment").d("loadState: $it")
                swipeRefresh.isRefreshing = it
            }
        }

        val eventDelegate = effectDelegate.create(viewModel as ViewModel).eventDelegate
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effect.collect {
                when (it) {
                    is TimelineEffect.ToTopOfList -> {
                        if (it.needsSkip) {
                            binding.mainList.scrollToPosition(4)
                        }
                        binding.mainList.smoothScrollToPosition(0)
                    }
                    else -> eventDelegate.accept(it)
                }
            }
        }

        val menuProvider = MenuProvider(viewModel)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.listState.map { it.isHeadingEnabled }.distinctUntilChanged()
                .observe(viewLifecycleOwner) {
                    menuProvider.headingItem?.isEnabled = it
                }
        }
        val menuHost = requireActivity() as MenuHost
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.listState.map { it.isHeadingVisible }.distinctUntilChanged()
                .observe(viewLifecycleOwner) {
                    if (it) {
                        menuHost.addMenuProvider(menuProvider, viewLifecycleOwner)
                    } else {
                        menuHost.removeMenuProvider(menuProvider)
                    }
                }
        }
    }

    private fun RecyclerView.setup(
        adapter: PagingDataAdapter<Any, *>,
        viewModel: ListItemLoadableViewModel<*>,
    ) {
        val linearLayoutManager = LinearLayoutManager(context)
        this.layoutManager = linearLayoutManager
        this.addItemDecoration(DividerItemDecoration(context, linearLayoutManager.orientation))
        this.adapter = adapter

        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val firstVisibleItemPosition =
                        linearLayoutManager.findFirstVisibleItemPosition()
                    viewModel.stopScrollingList.dispatch(firstVisibleItemPosition)
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    viewModel.scrollList.dispatch()
                }
            }
        })
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.timeline.collectLatest(adapter::submitData)
        }
    }

    private val args: ListItemFragmentArgs by navArgs()
    private val ownerId: Int get() = args.ownerId.value
    private val listOwner: ListOwner<*> get() = ListOwner(ownerId, args.query)

    companion object {
        fun newInstance(owner: ListOwner<*>, label: String = ""): ListItemFragment {
            return ListItemFragment().apply {
                arguments = bundle(owner, label)
            }
        }

        fun bundle(owner: ListOwner<*>, label: String): Bundle {
            return ListItemFragmentArgs(owner.query, owner.id, label).toBundle()
        }
    }

    private class MenuProvider(
        private val viewModel: ListItemLoadableViewModel<*>,
    ) : androidx.core.view.MenuProvider {
        var headingItem: MenuItem? = null
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            val be = menu.children.filter { it.itemId == R.id.action_heading }.toSet()
            menuInflater.inflate(R.menu.timeline, menu)
            val af = menu.children.filter { it.itemId == R.id.action_heading }.toSet()

            check((af - be).size == 1)
            val headingItem = (af - be).first()
            val state = viewModel.listState.value ?: return
            this.headingItem = headingItem.apply {
                isVisible = state.isHeadingVisible
                isEnabled = state.isHeadingEnabled
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.action_heading -> {
                    viewModel.heading.dispatch()
                    true
                }
                else -> false
            }
        }
    }
}

interface ListItemFragmentEffectDelegate : ActivityEffectDelegate {
    override fun accept(event: AppEffect)
}
