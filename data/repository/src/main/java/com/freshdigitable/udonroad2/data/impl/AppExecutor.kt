package com.freshdigitable.udonroad2.data.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class AppExecutor(
    private val parentJob: Job = SupervisorJob(),
    val dispatcher: DispatcherProvider = DispatcherProvider(),
    coroutineScope: CoroutineScope = CoroutineScope(dispatcher.mainDispatcher + parentJob)
) : CoroutineScope by coroutineScope {
    val io: Executor = Executor { command ->
        launchIO { command.run() }
    }

    fun launchIO(task: suspend CoroutineScope.() -> Unit) {
        launch(dispatcher.ioDispatcher, block = task)
    }
}

class DispatcherProvider(
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
)
