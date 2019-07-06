package com.freshdigitable.udonroad2.navigation

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.observe
import com.freshdigitable.udonroad2.model.ActivityScope
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
    protected val activity: AppCompatActivity,
    @IdRes
    protected val containerId: Int
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

    protected fun replace(fragment: Fragment, backStackTag: String? = null) {
        val transaction = activity.supportFragmentManager.beginTransaction()
        transaction.replace(containerId, fragment)
        if (backStackTag != null) {
            transaction.addToBackStack(backStackTag)
        }
        transaction.commit()
    }

    protected fun isStackedOnTop(name: String): Boolean {
        val supportFragmentManager = activity.supportFragmentManager
        val index = supportFragmentManager.backStackEntryCount - 1
        if (index < 0) {
            return false
        }
        return supportFragmentManager.getBackStackEntryAt(index).name == name
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        eventDisposable.dispose()
    }
}
