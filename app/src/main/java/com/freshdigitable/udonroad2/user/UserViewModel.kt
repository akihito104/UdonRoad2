package com.freshdigitable.udonroad2.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlin.math.min

class UserViewModel(
    private val repository: UserRepository,
    private val relationshipRepository: RelationshipRepository
) : ViewModel() {
    private val userId: MutableLiveData<UserId> = MutableLiveData()
    val user: LiveData<User?> = userId.switchMap { repository.getUser(it) }
    val relationship: LiveData<Relationship?> = userId.switchMap {
        relationshipRepository.findRelationship(it)
    }

    fun setUserId(id: UserId) {
        userId.value = id
    }

    private val appBarScrollRate = MutableLiveData<Float>()
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
        relationshipRepository.updateFollowingStatus(userId.value!!, following)
    }

    fun updateBlockingStatus(blocking: Boolean) {
        relationshipRepository.updateBlockingStatus(userId.value!!, blocking)
    }

    fun updateMutingStatus(muting: Boolean) {
        relationshipRepository.updateMutingStatus(userId.value!!, muting)
    }

    fun updateWantRetweet(wantRetweet: Boolean) {
        relationshipRepository.updateWantRetweetStatus(relationship.value!!, wantRetweet)
    }

    fun reportForSpam() {
        relationshipRepository.reportSpam(userId.value!!)
    }
}

@Module
interface UserViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(UserViewModel::class)
    fun bindUserViewModel(viewModel: UserViewModel): ViewModel

    companion object {
        @Provides
        fun provideUserViewModel(
            userRepository: UserRepository,
            relationshipRepository: RelationshipRepository
        ): UserViewModel {
            return UserViewModel(userRepository, relationshipRepository)
        }
    }
}
