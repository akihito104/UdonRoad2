package com.freshdigitable.udonroad2.model.app.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.toLiveData
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.Serializable
import javax.inject.Inject

@ActivityScope
class EventDispatcher @Inject constructor() {
    val emitter = PublishSubject.create<NavigationEvent>()

    fun postEvent(event: NavigationEvent) {
        Timber.tag("NavigationDispatcher").d("postEvent: $event")
        emitter.onNext(event)
    }
}

typealias AppAction<T> = Observable<T>
typealias AppViewState<T> = LiveData<T>

inline fun <T> EventDispatcher.toAction(
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

inline fun <reified T> AppAction<out NavigationEvent>.filterByType(): AppAction<T> {
    return this.filter { it is T }.cast(T::class.java)
}

data class EventResult<T>(
    val event: NavigationEvent,
    private val result: Result<T>
) : Serializable {
    val value: T? = result.getOrNull()
    val isSuccess: Boolean
        get() = result.isSuccess
    val exception: Throwable?
        get() = result.exceptionOrNull()

    companion object {
        fun <T> success(event: NavigationEvent, value: T): EventResult<T> {
            return EventResult(event, Result.success(value))
        }

        fun <T> failure(event: NavigationEvent, throwable: Throwable): EventResult<T> {
            return EventResult(event, Result.failure(throwable))
        }
    }
}

typealias AppResult<T> = Observable<EventResult<T>>

fun Disposable.addTo(compositeDisposable: CompositeDisposable) {
    compositeDisposable.add(this)
}
