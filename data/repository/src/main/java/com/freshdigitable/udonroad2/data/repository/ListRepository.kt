package com.freshdigitable.udonroad2.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.db.dao.ListDao
import com.freshdigitable.udonroad2.data.restclient.ListRestClient
import com.freshdigitable.udonroad2.data.restclient.ListRestClientProvider
import com.freshdigitable.udonroad2.model.ListQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ListRepository<I> {
    val loading: LiveData<Boolean>

    fun getList(owner: String, query: ListQuery): LiveData<PagedList<I>>
    fun loadAtFront()
    fun clear()
}

abstract class ListRepositoryImpl<E, I>(
    private val dao: ListDao<E, I>,
    private val fetcher: ListFetcher<ListQuery, E, ListRestClient<ListQuery, E>, I>,
    private val clientProvider: ListRestClientProvider,
    private val executor: AppExecutor
) : ListRepository<I> {

    private val _loading = MutableLiveData<Boolean>()
    override val loading: LiveData<Boolean> = _loading

    companion object {
        private val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(20)
            .setInitialLoadSizeHint(100)
            .build()
    }

    private val owner = MutableLiveData<String>()
    private val listTable: MutableMap<String, LiveData<PagedList<I>>> = mutableMapOf()

    private val timeline: LiveData<PagedList<I>> = Transformations.switchMap(owner) {
        listTable.getOrPut(it) { getPagedList(it) }
    }

    private lateinit var apiClient: ListRestClient<ListQuery, E>

    override fun getList(owner: String, query: ListQuery): LiveData<PagedList<I>> {
        apiClient = clientProvider.get(query)
        this.owner.value = owner
        return timeline
    }

    private fun getPagedList(owner: String): LiveData<PagedList<I>> {
        val timeline = dao.getList(owner)
        return LivePagedListBuilder(timeline, config)
            .setFetchExecutor(executor.disk)
            .setBoundaryCallback(object : PagedList.BoundaryCallback<I>() {
                override fun onZeroItemsLoaded() {
                    super.onZeroItemsLoaded()
                    fetchTimeline(owner, fetcher.fetchOnZeroItems)
                }

                override fun onItemAtEndLoaded(itemAtEnd: I) {
                    super.onItemAtEndLoaded(itemAtEnd)
                    fetchTimeline(owner, fetcher.fetchOnBottom(itemAtEnd))
                }
            })
            .build()
    }

    override fun loadAtFront() {
        val owner = requireNotNull(this.owner.value) {
            "owner should be set before calling loadAtFront()."
        }

        val item = timeline.value?.getOrNull(0)
        if (item != null) {
            fetchTimeline(owner, fetcher.fetchOnTop(item))
        } else {
            fetchTimeline(owner, fetcher.fetchOnZeroItems)
        }
    }

    private fun fetchTimeline(
        owner: String,
        block: suspend ListRestClient<ListQuery, E>.() -> List<E>
    ) = GlobalScope.launch(Dispatchers.Default) {
        _loading.postValue(true)
        runCatching {
            val timeline = block(apiClient)
            withContext(Dispatchers.IO) {
                dao.addEntities(timeline, owner)
            }
        }.onSuccess {
            _loading.postValue(false)
        }.onFailure { e ->
            _loading.postValue(false)
            Log.e("TweetTimelineRepository", "fetchTimeline: ", e)
        }
    }

    override fun clear() {
        diskAccess {
            listTable.keys.forEach { dao.clean(it) }
        }
    }
}
