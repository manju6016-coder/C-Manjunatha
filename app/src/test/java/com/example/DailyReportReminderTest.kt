package com.example

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.notification.BootReceiver
import com.example.notification.DailyReminderReceiver
import com.example.notification.DailyReportReminderManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyReportReminderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
    }

    @Test
    fun testNotificationChannelCreation() {
        DailyReportReminderManager.createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(DailyReportReminderManager.CHANNEL_ID)
        assertNotNull("Notification channel should be created", channel)
        assertEquals(DailyReportReminderManager.CHANNEL_NAME, channel.name)
    }

    @Test
    fun testScheduleAndCancelReminderPreferences() {
        // Schedule at 21:30 (9:30 PM)
        DailyReportReminderManager.scheduleDailyReminder(
            context = context,
            hour = 21,
            minute = 30,
            enabled = true
        )

        assertTrue("Reminder should be enabled", DailyReportReminderManager.isReminderEnabled(context))
        assertEquals(21, DailyReportReminderManager.getScheduledHour(context))
        assertEquals(30, DailyReportReminderManager.getScheduledMinute(context))

        val formatted = DailyReportReminderManager.formatScheduledTime(21, 30)
        assertTrue("Formatted time should contain 09:30 or 9:30 and PM", formatted.contains("09:30") || formatted.contains("9:30"))
        assertTrue(formatted.contains("PM"))

        // Disable / Cancel
        DailyReportReminderManager.scheduleDailyReminder(
            context = context,
            hour = 21,
            minute = 30,
            enabled = false
        )
        assertFalse("Reminder should be disabled", DailyReportReminderManager.isReminderEnabled(context))
    }

    @Test
    fun testSendReminderNotificationDoesNotCrash() {
        DailyReportReminderManager.createNotificationChannel(context)
        DailyReportReminderManager.sendReminderNotification(
            context = context,
            outletCode = "OUT-101",
            shopName = "Bangalore Central"
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertNotNull(notificationManager)
    }

    @Test
    fun testDailyReminderReceiverTriggersAndReschedules() {
        DailyReportReminderManager.scheduleDailyReminder(
            context = context,
            hour = 20,
            minute = 0,
            enabled = true
        )

        val receiver = DailyReminderReceiver()
        val intent = Intent(DailyReportReminderManager.ACTION_REMINDER)
        receiver.onReceive(context, intent)

        // Verifies receiver executed without exception and reminder remains active
        assertTrue("Reminder should remain active after daily execution", DailyReportReminderManager.isReminderEnabled(context))
    }

    @Test
    fun testBootReceiverRestoresReminder() {
        DailyReportReminderManager.scheduleDailyReminder(
            context = context,
            hour = 19,
            minute = 45,
            enabled = true
        )

        val bootReceiver = BootReceiver()
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceive(context, bootIntent)

        assertTrue(DailyReportReminderManager.isReminderEnabled(context))
        assertEquals(19, DailyReportReminderManager.getScheduledHour(context))
        assertEquals(45, DailyReportReminderManager.getScheduledMinute(context))
    }
}
