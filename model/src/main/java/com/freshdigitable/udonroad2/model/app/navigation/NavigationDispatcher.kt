package com.freshdigitable.udonroad2.model.app.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.observe
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

@ActivityScope
class NavigationDispatcher @Inject constructor() {
    internal val emitter = PublishSubject.create<NavigationEvent>()

    fun postEvent(event: NavigationEvent) {
        emitter.onNext(event)
    }
}

interface NavigationEvent

interface FragmentContainerState

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
