package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.security.SecureRandom

/**
 * Service to generate, store, and dispatch One-Time Passwords (OTP)
 * for Admin (+91 8686122299), RIC In-Charges, and Shop Employees.
 *
 * It sends local high-priority notifications with the OTP and provides
 * full in-app alert feedback.
 */
object OtpAuthService {

    private const val TAG = "OtpAuthService"
    const val CHANNEL_ID = "channel_otp_auth"
    const val CHANNEL_NAME = "Login OTP Security Codes"
    const val NOTIFICATION_ID = 8686

    const val ADMIN_DEFAULT_MOBILE = "+91 8686122299"

    data class ActiveOtp(
        val mobileNumber: String,
        val otpCode: String,
        val createdAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes validity
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() > expiresAt
    }

    private var currentOtp: ActiveOtp? = null

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends security verification OTP codes for Login"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Generates a 6-digit numeric OTP code.
     */
    fun generateOtpCode(): String {
        val random = SecureRandom()
        val num = 100000 + random.nextInt(900000)
        return num.toString()
    }

    /**
     * Requests OTP for given mobile number, stores it in active state,
     * and dispatches a system notification.
     */
    fun sendOtp(
        context: Context,
        mobileNumber: String,
        userName: String,
        userRole: String
    ): String {
        createNotificationChannel(context)
        val otpCode = generateOtpCode()
        currentOtp = ActiveOtp(
            mobileNumber = normalizeMobile(mobileNumber),
            otpCode = otpCode
        )

        // Dispatch local notification
        dispatchOtpNotification(context, mobileNumber, userName, userRole, otpCode)

        Log.d(TAG, "Generated OTP $otpCode for $mobileNumber ($userName)")
        return otpCode
    }

    /**
     * Verifies the OTP entered by the user.
     */
    fun verifyOtp(mobileNumber: String, enteredOtp: String): Boolean {
        val active = currentOtp ?: return false
        if (active.isExpired) {
            currentOtp = null
            return false
        }
        val cleanEntered = enteredOtp.trim()
        val cleanMobile = normalizeMobile(mobileNumber)
        val matches = active.mobileNumber == cleanMobile && active.otpCode == cleanEntered
        if (matches) {
            currentOtp = null // Consume OTP once verified
        }
        return matches
    }

    fun getActiveOtpForMobile(mobileNumber: String): String? {
        val active = currentOtp ?: return null
        return if (!active.isExpired && active.mobileNumber == normalizeMobile(mobileNumber)) {
            active.otpCode
        } else {
            null
        }
    }

    fun clearOtp() {
        currentOtp = null
    }

    private fun dispatchOtpNotification(
        context: Context,
        mobile: String,
        name: String,
        role: String,
        otp: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Login OTP: $otp"
        val message = "Your verification OTP for $name ($role) on $mobile is $otp. Valid for 5 minutes. Do not share with anyone."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Your OTP is $otp for $mobile ($role)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle(title)
                    .setSummaryText("Outlet Ledger Security")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF10B981.toInt()) // EmeraldGreen
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted for OTP", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display OTP notification", e)
        }
    }

    /**
     * Normalizes mobile numbers by stripping spaces, hyphens, and +91 prefix.
     */
    fun normalizeMobile(mobile: String): String {
        val digitsOnly = mobile.filter { it.isDigit() }
        return if (digitsOnly.length > 10) {
            digitsOnly.takeLast(10)
        } else {
            digitsOnly
        }
    }
}
