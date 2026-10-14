package com.meettrack.app.di

import android.content.Context
import androidx.room.Room
import com.meettrack.app.data.db.ClientDao
import com.meettrack.app.data.db.MeetingDao
import com.meettrack.app.data.db.MeetTrackDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeetTrackDatabase =
        Room.databaseBuilder(
            context,
            MeetTrackDatabase::class.java,
            "meettrack_database"
        ).build()

    @Provides
    fun provideClientDao(db: MeetTrackDatabase): ClientDao = db.clientDao()

    @Provides
    fun provideMeetingDao(db: MeetTrackDatabase): MeetingDao = db.meetingDao()
}
