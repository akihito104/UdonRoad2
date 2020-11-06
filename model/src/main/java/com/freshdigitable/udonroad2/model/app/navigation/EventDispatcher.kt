package com.freshdigitable.udonroad2.model.app.navigation

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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

inline fun <reified T> EventDispatcher.toAction(
    block: PublishSubject<AppEvent>.() -> AppAction<T> = { filterByType() }
): AppAction<T> {
    return PublishSubject.create<T>().also { action ->
        this.emitter.block().subscribe(action)
    }
}

fun Disposable.addTo(compositeDisposable: CompositeDisposable) {
    compositeDisposable.add(this)
}
