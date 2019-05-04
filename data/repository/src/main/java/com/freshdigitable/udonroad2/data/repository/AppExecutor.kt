package com.freshdigitable.udonroad2.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

class AppExecutor {
    val disk: Executor = Executor {
        diskAccess { it.run() }
    }

    val network: Executor = Executor {
        GlobalScope.launch(Dispatchers.Default) {
            it.run()
        }
    }

    fun diskIO(task: () -> Unit) = diskAccess(task)
}

internal fun diskAccess(task: () -> Unit) = GlobalScope.launch(Dispatchers.IO) {
    task()
}

internal suspend fun <T> networkAccess(callable: () -> T): T = coroutineScope {
    withContext(Dispatchers.Default) {
        callable()
    }
}
