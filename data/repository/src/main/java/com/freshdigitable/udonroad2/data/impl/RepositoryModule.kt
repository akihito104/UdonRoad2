package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.db.LocalListDataSourceModule
import com.freshdigitable.udonroad2.data.db.PagedListDataSourceFactoryModule
import com.freshdigitable.udonroad2.data.restclient.MemberListDataSourceModule
import com.freshdigitable.udonroad2.data.restclient.TweetTimelineDataSourceModule
import com.freshdigitable.udonroad2.model.RepositoryScope
import dagger.Module
import dagger.Subcomponent

@RepositoryScope
@Subcomponent(
    modules = [
        TweetRepositoryModule::class,
        UserRepositoryModule::class,
        RelationshipRepositoryModule::class,
        OAuthTokenRepositoryModule::class
    ]
)
interface RepositoryComponent {

    @RepositoryScope
    fun tweetRepository(): TweetRepository

    @RepositoryScope
    fun userRepository(): UserRepository

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

@Module(
    includes = [
        LocalListDataSourceModule::class,
        TweetTimelineDataSourceModule::class,
        MemberListDataSourceModule::class,
        PagedListDataSourceFactoryModule::class
    ]
)
interface ListRepositoryModule
