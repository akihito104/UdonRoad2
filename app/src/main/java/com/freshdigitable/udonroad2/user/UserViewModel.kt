package com.freshdigitable.udonroad2.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import kotlin.math.min

class UserViewModel(
    private val userId: UserId,
    userRepository: UserRepository,
    private val relationshipRepository: RelationshipRepository
) : ViewModel() {
    val user: LiveData<User?> = userRepository.getUser(userId)
    val relationship: LiveData<Relationship?> = relationshipRepository.findRelationship(userId)

    private val appBarScrollRate = MutableLiveData(0f)
    val titleAlpha: LiveData<Float> = appBarScrollRate.map { r ->
        if (r >= 0.9f) {
            min((r - 0.9f) * 10, 1f)
        } else {
            0f
        }
    }

    fun setAppBarScrollRate(rate: Float) {
        appBarScrollRate.value = rate
    }

    private val currentPage = MutableLiveData(0)
    private val selectedItemId = MutableLiveData<MutableMap<Int, SelectedItemId?>>(mutableMapOf())

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

    fun updateFollowingStatus(following: Boolean) {
        relationshipRepository.updateFollowingStatus(userId, following)
    }

    fun updateBlockingStatus(blocking: Boolean) {
        relationshipRepository.updateBlockingStatus(userId, blocking)
    }

    fun updateMutingStatus(muting: Boolean) {
        relationshipRepository.updateMutingStatus(userId, muting)
    }

    fun updateWantRetweet(wantRetweet: Boolean) {
        relationshipRepository.updateWantRetweetStatus(relationship.value!!, wantRetweet)
    }

    fun reportForSpam() {
        relationshipRepository.reportSpam(userId)
    }
}

@Module
interface UserViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(UserViewModel::class)
        fun provideUserViewModel(
            userId: UserId,
            userRepository: UserRepository,
            relationshipRepository: RelationshipRepository
        ): ViewModel {
            return UserViewModel(userId, userRepository, relationshipRepository)
        }
    }
}

@Subcomponent(modules = [UserViewModelModule::class])
interface UserViewModelComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance userId: UserId): UserViewModelComponent
    }

    val viewModelProvider: ViewModelProvider
}

@Module(subcomponents = [UserViewModelComponent::class])
interface UserViewModelComponentModule
