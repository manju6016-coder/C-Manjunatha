package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("DailyReminderReceiver", "Received broadcast action: $action")

        if (action == DailyReportReminderManager.ACTION_REMINDER) {
            if (DailyReportReminderManager.isReminderEnabled(context)) {
                // Send notification to employee
                DailyReportReminderManager.sendReminderNotification(context)

                // Reschedule for next day at the same configured time
                val hour = DailyReportReminderManager.getScheduledHour(context)
                val minute = DailyReportReminderManager.getScheduledMinute(context)
                DailyReportReminderManager.scheduleDailyReminder(context, hour, minute, true)
            }
        }
    }
}
