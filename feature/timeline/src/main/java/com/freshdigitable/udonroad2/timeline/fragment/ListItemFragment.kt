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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.databinding.FragmentTimelineBinding
import com.freshdigitable.udonroad2.timeline.di.ListItemAdapterComponent
import com.freshdigitable.udonroad2.timeline.di.ListItemFragmentEventDelegateComponent
import com.freshdigitable.udonroad2.timeline.di.ListItemViewModelComponent
import com.freshdigitable.udonroad2.timeline.di.viewModel
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class ListItemFragment : Fragment() {
    @Inject
    lateinit var listItemViewModelBuilder: ListItemViewModelComponent.Builder

    @Inject
    lateinit var listItemAdapterFactory: ListItemAdapterComponent.Factory

    @Inject
    lateinit var eventDelegate: ListItemFragmentEventDelegateComponent.Factory
    private lateinit var viewModel: ListItemLoadableViewModel<*, Any>

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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

        val adapter = listItemAdapterFactory.create(viewModel, viewLifecycleOwner)
            .adapter as PagingDataAdapter<Any, *>
        binding.mainList.setup(adapter, viewModel)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.timeline.collectLatest(adapter::submitData)
        }
        val swipeRefresh = binding.mainSwipeRefresh
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            adapter.loadStateFlow.collectLatest {
                swipeRefresh.isRefreshing = it.refresh is LoadState.Loading ||
                    it.append is LoadState.Loading ||
                    it.prepend is LoadState.Loading
            }
        }

        val eventDelegate = eventDelegate.create(viewModel).eventDelegate
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigationEvent.collect(eventDelegate::dispatchNavHostNavigate)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.feedbackMessage.collect(eventDelegate::dispatchFeedbackMessage)
        }
    }

    private fun RecyclerView.setup(
        adapter: PagingDataAdapter<*, *>,
        viewModel: ListItemLoadableViewModel<*, Any>
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
                    viewModel.onListScrollStopped(firstVisibleItemPosition)
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    viewModel.onListScrollStarted()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.timeline, menu)

        val headingItem = menu.findItem(R.id.action_heading)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.isHeadingEnabled.collect {
                headingItem.isEnabled = it
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_heading -> {
                viewModel.onHeadingClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
}

interface ListItemFragmentEventDelegate : ActivityEventDelegate {
    override fun dispatchNavHostNavigate(event: NavigationEvent)
    override fun dispatchFeedbackMessage(message: FeedbackMessage)
}
