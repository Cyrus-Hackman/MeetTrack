package com.meettrack.app.di

import com.meettrack.app.notifications.NotificationScheduler
import com.meettrack.app.notifications.ReminderRescheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    abstract fun bindReminderRescheduler(
        notificationScheduler: NotificationScheduler
    ): ReminderRescheduler
}
