package com.meettrack.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.meettrack.app.data.db.MeetTrackDatabase
import com.meettrack.app.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var database: MeetTrackDatabase

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Boot/update received, re-scheduling reminders")
            val pendingResult = goAsync()
            applicationScope.launch {
                try {
                    // Re-schedule all upcoming meetings
                    val meetings = database.meetingDao().getAllUpcomingMeetingsDirect()
                    meetings.forEach { meetingWithClient ->
                        notificationScheduler.scheduleReminders(meetingWithClient)
                    }
                    Log.d("BootReceiver", "Re-scheduled ${meetings.size} meeting reminders")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to re-schedule reminders", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
