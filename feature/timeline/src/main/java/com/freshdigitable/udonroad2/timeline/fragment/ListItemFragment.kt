package com.freshdigitable.udonroad2.timeline.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.di.FragmentScope
import com.freshdigitable.udonroad2.timeline.ListItemAdapterComponent
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.ListItemViewModelComponent
import com.freshdigitable.udonroad2.timeline.ListOwner
import com.freshdigitable.udonroad2.timeline.databinding.FragmentTimelineBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class ListItemFragment : Fragment() {
    @Inject
    lateinit var listItemViewModelBuilder: ListItemViewModelComponent.Builder

    @Inject
    lateinit var listItemAdapterFactory: ListItemAdapterComponent.Factory

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DataBindingUtil.findBinding<FragmentTimelineBinding>(view) ?: return
        binding.lifecycleOwner = viewLifecycleOwner

        val listOwner = ListOwner(ownerId, query)
        val viewModelComponent = listItemViewModelBuilder
            .owner(listOwner)
            .savedStateRegistryOwner(this)
            .firstArgs(savedInstanceState)
            .build()
        val viewModelProvider = ViewModelProvider(
            this,
            viewModelComponent.savedStateViewModelProviderFactory()
        )
        val viewModel: ViewModel =
            viewModelProvider.get("_$ownerId", viewModelComponent.viewModelClass())
        binding.viewModel = viewModel as ListItemLoadable<*, Any>

        val listView = binding.mainList
        val linearLayoutManager = LinearLayoutManager(view.context)
        listView.layoutManager = linearLayoutManager
        listView.addItemDecoration(
            DividerItemDecoration(
                view.context,
                linearLayoutManager.orientation
            )
        )
        val adapter =
            listItemAdapterFactory.create(viewModel, viewLifecycleOwner)
                .adapter() as PagedListAdapter<Any, *>
        listView.adapter = adapter
        viewModel.timeline.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

    private val ownerId: Int
        get() = requireArguments().getInt(ARGS_OWNER_ID)

    private val query: QueryType
        get() = requireArguments().getSerializable(ARGS_QUERY) as QueryType

    companion object {
        private val ownerIdGen = AtomicInteger(0)
        const val ARGS_OWNER_ID = "owner_id"
        const val ARGS_QUERY = "query"

        fun newInstance(query: QueryType): ListItemFragment {
            return ListItemFragment().apply {
                arguments = bundle(query)
            }
        }

        fun bundle(query: QueryType): Bundle {
            return bundleOf(
                ARGS_QUERY to query,
                ARGS_OWNER_ID to ownerIdGen.getAndIncrement()
            )
        }
    }
}

@Module
interface ListItemFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributeListItemFragment(): ListItemFragment
}
