package com.freshdigitable.udonroad2.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import kotlin.math.min

class UserViewModel(
    private val tweetingUser: TweetingUser,
    private val eventDispatcher: EventDispatcher,
    private val viewState: UserActivityViewStates,
    userRepository: UserRepository,
    relationshipRepository: RelationshipRepository,
    selectedItemRepository: SelectedItemRepository,
    ownerGenerator: ListOwnerGenerator,
) : ViewModel() {
    val user: LiveData<User?> = userRepository.getUser(tweetingUser.id)
    val relationship: LiveData<Relationship?> = relationshipRepository.findRelationship(
        tweetingUser.id
    )

    // TODO: save to state handle
    private val pages: Map<UserPage, ListOwner<*>> = UserPage.values().map {
        it to ownerGenerator.create(it.createQuery(tweetingUser))
    }.toMap()

    fun getOwner(userPage: UserPage): ListOwner<*> {
        return requireNotNull(pages[userPage])
    }

    fun setAppBarScrollRate(rate: Float) {
        appBarScrollRate.value = rate
    }

    private val appBarScrollRate = MutableLiveData(0f)
    val titleAlpha: LiveData<Float> = appBarScrollRate.map { r ->
        if (r >= 0.9f) {
            min((r - 0.9f) * 10, 1f)
        } else {
            0f
        }
    }

    fun setCurrentPage(index: Int) {
        this.currentPage.value = UserPage.values()[index]
    }

    private val currentPage = MutableLiveData(UserPage.values()[0])
    private val selectedItemId = currentPage.switchMap {
        selectedItemRepository.observe(requireNotNull(pages[it]))
    }
    val fabVisible: LiveData<Boolean> = selectedItemId.map { it != null }

    fun updateFollowingStatus(following: Boolean) {
        eventDispatcher.postEvent(UserActivityEvent.Following(following, tweetingUser.id))
    }

    fun updateBlockingStatus(blocking: Boolean) {
        eventDispatcher.postEvent(UserActivityEvent.Blocking(blocking, tweetingUser.id))
    }

    fun updateMutingStatus(muting: Boolean) {
        eventDispatcher.postEvent(UserActivityEvent.Muting(muting, tweetingUser.id))
    }

    fun updateWantRetweet(wantRetweet: Boolean) {
        eventDispatcher.postEvent(UserActivityEvent.WantsRetweet(wantRetweet, tweetingUser.id))
    }

    fun reportForSpam() {
        eventDispatcher.postEvent(UserActivityEvent.ReportSpam(tweetingUser.id))
    }
}

@Module
interface UserViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(UserViewModel::class)
        fun provideUserViewModel(
            user: TweetingUser,
            eventDispatcher: EventDispatcher,
            viewState: UserActivityViewStates,
            userRepository: UserRepository,
            relationshipRepository: RelationshipRepository,
            selectedItemRepository: SelectedItemRepository,
            ownerGenerator: ListOwnerGenerator,
        ): ViewModel {
            return UserViewModel(
                user,
                eventDispatcher,
                viewState,
                userRepository,
                relationshipRepository,
                selectedItemRepository,
                ownerGenerator
            )
        }
    }
}

@Subcomponent(modules = [UserViewModelModule::class])
interface UserViewModelComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance user: TweetingUser): UserViewModelComponent
    }

    val viewModelProvider: ViewModelProvider
}

@Module(subcomponents = [UserViewModelComponent::class])
interface UserViewModelComponentModule
