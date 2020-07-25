package com.freshdigitable.udonroad2.model.app.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.observe
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

@ActivityScope
class NavigationDispatcher @Inject constructor() {
    val emitter = PublishSubject.create<NavigationEvent>()

    fun postEvent(event: NavigationEvent) {
        emitter.onNext(event)
    }
}

typealias AppAction<T> = Observable<T>
typealias AppViewState<T> = Observable<T>

data class StateHolder<T>(val value: T?)

inline fun <T> NavigationDispatcher.toAction(
    block: PublishSubject<NavigationEvent>.() -> AppAction<T>
): AppAction<T> {
    return PublishSubject.create<T>().also { action ->
        this.emitter.block().subscribe(action)
    }
}

inline fun <T, reified E> AppAction<T>.toViewState(
    block: AppAction<T>.() -> AppViewState<E> = { cast(E::class.java) }
): AppViewState<E> {
    return BehaviorSubject.create<E>().also { action ->
        this.block().subscribe(action)
    }
}

inline fun <reified T> Observable<NavigationEvent>.filterByType(): Observable<T> {
    return this.filter { it is T }.cast(T::class.java)
}

inline fun <reified T> Flowable<NavigationEvent>.filterByType(): Flowable<T> {
    return this.filter { it is T }.cast(T::class.java)
}

interface NavigationEvent

interface FragmentContainerState

interface ViewState {
    val containerState: FragmentContainerState
}

sealed class CommonEvent : NavigationEvent {
    data class Back(val currentState: ViewState?) : CommonEvent()
}

abstract class Navigation<T : FragmentContainerState>(
    val navigator: NavigationDispatcher,
    protected val activity: AppCompatActivity
) : LifecycleObserver {

    private val state: MutableLiveData<T?> = MutableLiveData()
    protected val currentState: T?
        get() = state.value

    private val eventDisposable: Disposable

    init {
        activity.lifecycle.addObserver(this)
        eventDisposable = navigator.emitter
            .subscribe { event ->
                val s = onEvent(event) ?: return@subscribe
                state.postValue(s)
            }

        state.observe(activity) { navigate(it) }
    }

    abstract fun onEvent(event: NavigationEvent): T?
    abstract fun navigate(s: T?)

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        eventDisposable.dispose()
    }
}
