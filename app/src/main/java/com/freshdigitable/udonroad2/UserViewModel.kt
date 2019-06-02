package com.freshdigitable.udonroad2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.data.repository.RepositoryComponent
import com.freshdigitable.udonroad2.data.repository.UserRepository
import com.freshdigitable.udonroad2.model.User
import com.freshdigitable.udonroad2.timeline.SelectedItemId
import dagger.Module
import dagger.Provides

class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {
    private val userId: MutableLiveData<Long> = MutableLiveData()
    val user: LiveData<User?> = Transformations.switchMap(userId) { repository.getUser(it) }

    fun setUserId(id: Long) {
        userId.value = id
    }

    private val appBarScrollRate = MutableLiveData<Float>()
    val titleAlpha: LiveData<Float> = Transformations.map(appBarScrollRate) { r ->
        if (r >= 0.9f) {
            Math.min((r - 0.9f) * 10, 1f)
        } else {
            0f
        }
    }

    fun setAppBarScrollRate(rate: Float) {
        appBarScrollRate.value = rate
    }

    private val currentPage = MutableLiveData<Int>().apply { value = 0 }
    private val selectedItemId =
        MutableLiveData<MutableMap<Int, SelectedItemId?>>().apply { value = mutableMapOf() }

    private val _fabVisible = MediatorLiveData<Boolean>().also {
        it.addSource(currentPage) { updateFabVisible() }
        it.addSource(selectedItemId) { updateFabVisible() }
    }
    val fabVisible: LiveData<Boolean> = _fabVisible

    fun setCurrentPage(index: Int) {
        this.currentPage.value = index
    }

    fun setSelectedItemId(selectedItemId: SelectedItemId?) {
        val map = requireNotNull(this.selectedItemId.value)
        val curr = requireNotNull(currentPage.value)
        map[curr] = selectedItemId
        this.selectedItemId.value = map
    }

    private fun updateFabVisible() {
        val map = requireNotNull(this.selectedItemId.value)
        val curr = requireNotNull(currentPage.value)
        _fabVisible.value = map[curr] != null
    }
}

@Module
object UserViewModelModule {
    @Provides
    @JvmStatic
    fun provideUserViewModel(repository: RepositoryComponent.Builder): UserViewModel {
        return UserViewModel(repository.build().userRepository())
    }
}
