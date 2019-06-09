package com.freshdigitable.udonroad2.timeline

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.UserListItem
import com.freshdigitable.udonroad2.timeline.databinding.FragmentTimelineBinding
import com.freshdigitable.udonroad2.timeline.databinding.ViewUserListItemBinding
import dagger.android.support.AndroidSupportInjection
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class UserListFragment : Fragment() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

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

        val viewModel = ViewModelProviders.of(this, factory).get(UserListViewModel::class.java)
        binding.viewModel = viewModel

        val listView = binding.mainList
        val linearLayoutManager = LinearLayoutManager(view.context)
        listView.layoutManager = linearLayoutManager
        listView.addItemDecoration(DividerItemDecoration(view.context, linearLayoutManager.orientation))
        val adapter = UserListAdapter(viewModel)
        listView.adapter = adapter
        viewModel.getTimeline(ListOwner(ownerId, query)).observe(viewLifecycleOwner, Observer { list ->
            adapter.submitList(list)
        })
    }

    private val ownerId: Int
        get() = requireArguments().getInt(ARGS_OWNER_ID)

    private val query: ListQuery
        get() = requireArguments().getSerializable(ARGS_QUERY) as ListQuery

    companion object {
        private val ownerIdGen = AtomicInteger(0)
        private const val ARGS_OWNER_ID = "owner_id"
        private const val ARGS_QUERY = "query"

        fun newInstance(query: ListQuery): UserListFragment {
            return UserListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARGS_QUERY, query)
                    putInt(ARGS_OWNER_ID, ownerIdGen.getAndIncrement())
                }
            }
        }
    }
}

private class UserListAdapter(
    private val viewModel: UserListViewModel
) : PagedListAdapter<UserListItem, UserListViewHolder>(diffUtil) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserListViewHolder =
        UserListViewHolder(ViewUserListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
        val item = getItem(position) ?: return

        holder.binding.user = item
    }

    override fun onViewAttachedToWindow(holder: UserListViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.binding.viewModel = viewModel
    }

    override fun onViewDetachedFromWindow(holder: UserListViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.binding.viewModel = null
    }

    override fun getItemViewType(position: Int): Int = R.layout.view_tweet_list_item

    override fun getItemId(position: Int): Long = getItem(position)?.id ?: -1
}

private val diffUtil = object : DiffUtil.ItemCallback<UserListItem>() {
    override fun areItemsTheSame(
        oldItem: UserListItem,
        newItem: UserListItem
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: UserListItem,
        newItem: UserListItem
    ): Boolean = oldItem == newItem
}

private class UserListViewHolder(
    internal val binding: ViewUserListItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    internal val root: ViewGroup = itemView as ViewGroup
}
