package com.woliveiras.petit.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** Hilt module that provides app-wide utilities. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides @Singleton fun provideClock(): Clock = Clock.systemDefaultZone()
}
