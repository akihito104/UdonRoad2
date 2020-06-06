package com.freshdigitable.udonroad2.timeline.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.databinding.FragmentTimelineBinding
import com.freshdigitable.udonroad2.timeline.ListOwner
import dagger.android.support.AndroidSupportInjection
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class ListItemFragment<T, Q: ListQuery, I> : Fragment()
    where T : ViewModel,
          T : ListItemLoadable<Q, I> {
    @Inject
    lateinit var viewModelProvider: ViewModelProvider
    protected abstract val viewModelClass: KClass<T>
    protected abstract fun createListAdapter(viewModel: T): PagedListAdapter<I, *>

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

        val viewModel = viewModelProvider.get("_$ownerId", viewModelClass.java)
        binding.viewModel = viewModel

        val listView = binding.mainList
        val linearLayoutManager = LinearLayoutManager(view.context)
        listView.layoutManager = linearLayoutManager
        listView.addItemDecoration(
            DividerItemDecoration(
                view.context,
                linearLayoutManager.orientation
            )
        )
        val adapter = createListAdapter(viewModel)
        listView.adapter = adapter
        viewModel.getList(
            ListOwner(
                ownerId,
                query
            )
        ).observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

    private val ownerId: Int
        get() = requireArguments().getInt(ARGS_OWNER_ID)

    private val query: Q
        get() = requireArguments().getSerializable(ARGS_QUERY) as Q

    companion object {
        val ownerIdGen = AtomicInteger(0)
        const val ARGS_OWNER_ID = "owner_id"
        const val ARGS_QUERY = "query"

        inline fun <reified T : ListItemFragment<*, *, *>> newInstance(query: ListQuery): T {
            return T::class.java.newInstance().apply {
                (this as Fragment).arguments = Bundle().apply {
                    putSerializable(ARGS_QUERY, query)
                    putInt(ARGS_OWNER_ID, ownerIdGen.getAndIncrement())
                }
            }
        }
    }
}
