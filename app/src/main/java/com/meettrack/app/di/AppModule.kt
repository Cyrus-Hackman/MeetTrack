package com.meettrack.app.di

import android.content.Context
import com.meettrack.app.data.preferences.SettingsRepository
import com.meettrack.app.notifications.NotificationScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideNotificationScheduler(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): NotificationScheduler = NotificationScheduler(context, settingsRepository)
}
