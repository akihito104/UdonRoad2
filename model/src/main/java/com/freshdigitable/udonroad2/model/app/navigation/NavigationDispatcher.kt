package com.freshdigitable.udonroad2.model.app.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.toLiveData
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

@ActivityScope
class NavigationDispatcher @Inject constructor() {
    val emitter = PublishSubject.create<NavigationEvent>()

    fun postEvent(event: NavigationEvent) {
        Timber.tag("NavigationDispatcher").d("postEvent: $event")
        emitter.onNext(event)
    }
}

typealias AppAction<T> = Observable<T>
typealias AppViewState<T> = LiveData<T>

inline fun <T> NavigationDispatcher.toAction(
    block: PublishSubject<NavigationEvent>.() -> AppAction<T>
): AppAction<T> {
    return PublishSubject.create<T>().also { action ->
        this.emitter.block().subscribe(action)
    }
}

inline fun <T, reified E> AppAction<T>.toViewState(
    block: AppAction<T>.() -> Observable<E> = { cast(E::class.java) }
): AppViewState<E> {
    return BehaviorSubject.create<E>().also { action ->
        this.block().subscribe(action)
    }
        .toFlowable(BackpressureStrategy.BUFFER)
        .toLiveData()
        .distinctUntilChanged()
}

inline fun <reified T> Observable<NavigationEvent>.filterByType(): Observable<T> {
    return this.filter { it is T }.cast(T::class.java)
}

inline fun <reified T> Flowable<NavigationEvent>.filterByType(): Flowable<T> {
    return this.filter { it is T }.cast(T::class.java)
}
