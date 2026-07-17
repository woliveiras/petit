package com.woliveiras.petit.di

import com.woliveiras.petit.util.LocaleApplicator
import com.woliveiras.petit.util.LocaleHelper
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

  @Provides
  @Singleton
  fun provideLocaleApplicator(localeHelper: LocaleHelper): LocaleApplicator = localeHelper
}
