package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DailyReportReminderManager {

    private const val TAG = "DailyReminderManager"
    const val CHANNEL_ID = "channel_daily_financial_report"
    const val CHANNEL_NAME = "Daily Financial Report Reminders"
    const val ACTION_REMINDER = "com.example.ACTION_DAILY_REPORT_REMINDER"
    const val NOTIFICATION_ID = 2026
    const val REQUEST_CODE_ALARM = 5001

    private const val PREF_NAME = "daily_report_reminder_prefs"
    private const val KEY_ENABLED = "reminder_enabled"
    private const val KEY_HOUR = "reminder_hour"
    private const val KEY_MINUTE = "reminder_minute"
    private const val KEY_LAST_SENT_DATE = "last_sent_date"

    // Default scheduled time: 20:00 (8:00 PM) - end of business day
    const val DEFAULT_HOUR = 20
    const val DEFAULT_MINUTE = 0

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, true)
    }

    fun getScheduledHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_HOUR, DEFAULT_HOUR)
    }

    fun getScheduledMinute(context: Context): Int {
        return getPrefs(context).getInt(KEY_MINUTE, DEFAULT_MINUTE)
    }

    fun formatScheduledTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Reminds store employees to complete and submit daily sales, purchases, and deposit logs."
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(
        context: Context,
        hour: Int = getScheduledHour(context),
        minute: Int = getScheduledMinute(context),
        enabled: Boolean = true
    ) {
        createNotificationChannel(context)

        getPrefs(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()

        if (!enabled) {
            cancelReminder(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If scheduled time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Daily reminder scheduled for ${formatScheduledTime(hour, minute)} (Epoch: ${calendar.timeInMillis})")
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed exact alarm permission, falling back to setAndAllowWhileIdle", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Error scheduling daily reminder", ex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily reminder", e)
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        Log.d(TAG, "Daily reminder cancelled")
    }

    fun sendReminderNotification(
        context: Context,
        outletCode: String? = null,
        shopName: String? = null
    ) {
        createNotificationChannel(context)

        // Open app on click
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val targetOutlet = if (!outletCode.isNullOrBlank()) " ($outletCode)" else ""
        val subtitle = if (!shopName.isNullOrBlank()) " for $shopName$targetOutlet" else ""

        val title = "Daily Report Submission Reminder"
        val message = "Friendly reminder to submit today's daily financial report$subtitle. Record purchases, sales, and bank deposits before closing."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Please submit today's daily financial report$subtitle.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle(title)
                    .setSummaryText("Daily Ledger Reminder")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setColor(0xFF3730A3.toInt()) // PrimaryIndigo
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, notification)

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            getPrefs(context).edit().putString(KEY_LAST_SENT_DATE, todayStr).apply()
            Log.d(TAG, "Reminder notification sent successfully")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing or denied", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification", e)
        }
    }
}
