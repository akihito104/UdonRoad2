package com.freshdigitable.udonroad2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.data.repository.RepositoryComponent
import com.freshdigitable.udonroad2.data.repository.UserRepository
import com.freshdigitable.udonroad2.model.User
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
}

@Module
object UserViewModelModule {
    @Provides
    @JvmStatic
    fun provideUserViewModel(repository: RepositoryComponent.Builder): UserViewModel {
        return UserViewModel(repository.build().userRepository())
    }
}
