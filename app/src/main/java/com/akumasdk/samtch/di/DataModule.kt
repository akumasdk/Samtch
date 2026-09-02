package com.akumasdk.samtch.di

import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.api.helix.HelixApi
import com.akumasdk.samtch.data.api.helix.HelixApiClient
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.data.settings.SettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // These are already annotated with @Inject and @Singleton,
    // so Hilt will pick them up automatically if they are in the same module or if we provide them.
    // However, since they were previously 'object', we might need to be explicit if they don't have @Inject on constructor.
    // I already added @Inject to their constructors.
}
