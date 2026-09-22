package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Boot completed: restoring daily report reminder")
            if (DailyReportReminderManager.isReminderEnabled(context)) {
                val hour = DailyReportReminderManager.getScheduledHour(context)
                val minute = DailyReportReminderManager.getScheduledMinute(context)
                DailyReportReminderManager.scheduleDailyReminder(context, hour, minute, true)
            }
        }
    }
}
