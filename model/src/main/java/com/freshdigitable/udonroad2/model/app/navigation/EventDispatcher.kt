package com.freshdigitable.udonroad2.model.app.navigation

import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@ActivityScope
class EventDispatcher @Inject constructor() {
    val emitter = PublishSubject.create<AppEvent>()

    fun postEvent(event: AppEvent) {
        Timber.tag("EventDispatcher").d("postEvent: $event")
        emitter.onNext(event)
    }
}

fun EventDispatcher.postEvents(vararg events: AppEvent) {
    events.forEach(this::postEvent)
}

inline fun <reified T : AppEvent> EventDispatcher.toActionFlow(
    crossinline prediction: (T) -> Boolean = { true },
): Flow<T> = callbackFlow {
    val disposable: AtomicReference<Disposable> = AtomicReference()
    val observer = object : Observer<AppEvent> {
        override fun onSubscribe(d: Disposable) {
            val isExpected = disposable.compareAndSet(null, d)
            if (!isExpected) d.dispose()
        }

        override fun onNext(t: AppEvent) {
            if (t is T && prediction(t)) {
                sendBlocking(t)
            }
        }

        override fun onError(e: Throwable) {
            close(e)
        }

        override fun onComplete() {
            close()
        }
    }

    emitter.subscribe(observer)
    awaitClose {
        val d = disposable.getAndSet(Disposables.disposed())
        if (!d.isDisposed) {
            Timber.tag("EventDispatcher").d("disposed(${T::class.simpleName}): $d")
            d.dispose()
        }
    }
}
