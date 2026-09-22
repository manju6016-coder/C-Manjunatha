package com.example.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists admin biometric security configuration preferences.
 */
class AdminBiometricPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Whether biometric security lock is enabled for Admin sensitive financial views.
     * Default is true so that admin access is secure by default.
     */
    var isBiometricLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_LOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, value).apply()

    /**
     * Timestamp of last successful biometric authentication in current app lifecycle.
     */
    var lastAuthenticatedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_AUTH_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_AUTH_TIMESTAMP, value).apply()

    /**
     * Clear active authentication session (e.g. on logout or manual lock).
     */
    fun clearSession() {
        prefs.edit().remove(KEY_LAST_AUTH_TIMESTAMP).apply()
    }

    companion object {
        private const val PREFS_NAME = "admin_biometric_security_prefs"
        private const val KEY_BIOMETRIC_LOCK_ENABLED = "key_biometric_lock_enabled"
        private const val KEY_LAST_AUTH_TIMESTAMP = "key_last_auth_timestamp"
    }
}
