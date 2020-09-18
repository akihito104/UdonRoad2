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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable
import timber.log.Timber
import java.io.Serializable
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ActivityScope
class EventDispatcher @Inject constructor() {
    val emitter = PublishSubject.create<AppEvent>()

    fun postEvent(event: AppEvent) {
        Timber.tag("NavigationDispatcher").d("postEvent: $event")
        emitter.onNext(event)
    }
}

fun EventDispatcher.postEvents(vararg events: AppEvent) {
    events.forEach(this::postEvent)
}

typealias AppAction<T> = Observable<T>
typealias AppViewState<T> = LiveData<T>

inline fun <reified T> EventDispatcher.toAction(
    block: PublishSubject<AppEvent>.() -> AppAction<T> = { filterByType() }
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

inline fun <reified T> AppAction<out AppEvent>.filterByType(): AppAction<T> {
    return this.filter { it is T }.cast(T::class.java)
}

@ExperimentalCoroutinesApi
inline fun <E : AppEvent, R> AppAction<E>.suspendMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (E) -> R
): AppAction<EventResult<R>> = flatMap { event ->
    rxObservable(coroutineContext) {
        val result = runCatching { block(event) }
        channel.send(EventResult(event, result))
    }
}

data class EventResult<T>(
    val event: AppEvent,
    private val result: Result<T>
) : Serializable {
    val value: T? = result.getOrNull()
    val isSuccess: Boolean
        get() = result.isSuccess
    val isFailure: Boolean
        get() = result.isFailure
    val exception: Throwable?
        get() = result.exceptionOrNull()

    companion object {
        fun <T> success(event: AppEvent, value: T): EventResult<T> {
            return EventResult(event, Result.success(value))
        }

        fun <T> failure(event: AppEvent, throwable: Throwable): EventResult<T> {
            return EventResult(event, Result.failure(throwable))
        }
    }
}

typealias AppResult<T> = Observable<EventResult<T>>

fun Disposable.addTo(compositeDisposable: CompositeDisposable) {
    compositeDisposable.add(this)
}
