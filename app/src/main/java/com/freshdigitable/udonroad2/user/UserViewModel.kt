package com.freshdigitable.udonroad2.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.user.UserActivityEvent.Relationships
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
) : ViewModel() {
    val user: LiveData<User?> = userRepository.getUser(tweetingUser.id)
    val relationship: LiveData<Relationship?> = relationshipRepository.findRelationship(
        tweetingUser.id
    )

    fun getOwner(userPage: UserPage): ListOwner<*> = requireNotNull(viewState.pages[userPage])

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

    val fabVisible: LiveData<Boolean> = viewState.fabVisible

    fun setCurrentPage(index: Int) {
        eventDispatcher.postEvent(UserActivityEvent.PageChanged(UserPage.values()[index]))
    }

    fun updateFollowingStatus(following: Boolean) {
        eventDispatcher.postEvent(Relationships.Following(following, tweetingUser.id))
    }

    fun updateBlockingStatus(blocking: Boolean) {
        eventDispatcher.postEvent(Relationships.Blocking(blocking, tweetingUser.id))
    }

    fun updateMutingStatus(muting: Boolean) {
        eventDispatcher.postEvent(Relationships.Muting(muting, tweetingUser.id))
    }

    fun updateWantRetweet(wantRetweet: Boolean) {
        eventDispatcher.postEvent(Relationships.WantsRetweet(wantRetweet, tweetingUser.id))
    }

    fun reportForSpam() {
        eventDispatcher.postEvent(Relationships.ReportSpam(tweetingUser.id))
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
        ): ViewModel = UserViewModel(
            user,
            eventDispatcher,
            viewState,
            userRepository,
            relationshipRepository,
        )
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
