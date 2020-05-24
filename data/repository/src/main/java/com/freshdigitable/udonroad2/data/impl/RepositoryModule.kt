package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.model.RepositoryScope
import dagger.Module
import dagger.Subcomponent

@RepositoryScope
@Subcomponent(
    modules = [
        TimelineRepositoryModule::class,
        UserListRepositoryModule::class,
        TweetRepositoryModule::class,
        UserRepositoryModule::class,
        MemberListListRepositoryModule::class,
        RelationshipRepositoryModule::class,
        OAuthTokenRepositoryModule::class
    ]
)
interface RepositoryComponent {
    @RepositoryScope
    fun tweetTimelineRepository(): TweetTimelineRepository

    @RepositoryScope
    fun userListRepository(): UserListRepository

    @RepositoryScope
    fun tweetRepository(): TweetRepository

    @RepositoryScope
    fun userRepository(): UserRepository

    @RepositoryScope
    fun memberListListRepository(): MemberListListRepository

    @RepositoryScope
    fun relationshipRepository(): RelationshipRepository

    @RepositoryScope
    fun oauthTokenRepository(): OAuthTokenRepository

    @Subcomponent.Builder
    interface Builder {
        fun build(): RepositoryComponent
    }
}

@Module(subcomponents = [RepositoryComponent::class])
interface RepositoryModule
