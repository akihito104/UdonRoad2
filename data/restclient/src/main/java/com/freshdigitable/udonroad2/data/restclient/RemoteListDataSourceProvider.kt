package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.ClassKeyMap
import com.freshdigitable.udonroad2.model.app.valueByAssignableClassObject
import javax.inject.Inject
import javax.inject.Provider

class RemoteListDataSourceProvider @Inject constructor(
    private val providers: ClassKeyMap<QueryType, Provider<RemoteListDataSource<out QueryType, *>>>
) {
    fun <Q : QueryType, T : RemoteListDataSource<Q, *>> get(query: Q): T {
        @Suppress("UNCHECKED_CAST")
        return providers.valueByAssignableClassObject(query).get() as T
    }
}
