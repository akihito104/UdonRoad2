package com.freshdigitable.udonroad2.data.repository

import com.freshdigitable.udonroad2.data.db.DatabaseModule
import com.freshdigitable.udonroad2.data.restclient.TwitterModule
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(
    modules = [
        DatabaseModule::class,
        TwitterModule::class
    ]
)
interface RepositoryComponent {
    fun homeTimelineRepository(): HomeTimelineRepository

    @Subcomponent.Builder
    interface Builder {
        fun build(): RepositoryComponent
    }
}

@Module(subcomponents = [RepositoryComponent::class])
object RepositoryModule {
    @Provides
    @JvmStatic
    fun provideHomeTimelineRepository(
        builder: RepositoryComponent.Builder
    ): HomeTimelineRepository = builder.build().homeTimelineRepository()
}
