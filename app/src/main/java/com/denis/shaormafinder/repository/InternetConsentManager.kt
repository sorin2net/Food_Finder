package com.denis.shaormafinder.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.edit


class InternetConsentManager(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences(
        "internet_consent_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_CONSENT_GIVEN = "internet_consent_given"
        private const val KEY_CONSENT_TIMESTAMP = "internet_consent_timestamp"
        private const val KEY_CONSENT_ASKED = "internet_consent_asked"
    }


    fun hasInternetConsent(): Boolean {
        val consent = sharedPreferences.getBoolean(KEY_CONSENT_GIVEN, false)
        return consent
    }


    fun hasAskedForConsent(): Boolean {
        return sharedPreferences.getBoolean(KEY_CONSENT_ASKED, false)
    }


    fun markConsentAsked() {
        sharedPreferences.edit {
            putBoolean(KEY_CONSENT_ASKED, true)
        }
    }


    fun grantConsent() {
        sharedPreferences.edit {
            putBoolean(KEY_CONSENT_GIVEN, true)
            putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            putBoolean(KEY_CONSENT_ASKED, true)
        }
    }


    fun revokeConsent() {
        sharedPreferences.edit {
            putBoolean(KEY_CONSENT_GIVEN, false)
        }
    }


    fun resetConsent() {
        sharedPreferences.edit {
            remove(KEY_CONSENT_GIVEN)
            remove(KEY_CONSENT_TIMESTAMP)
            remove(KEY_CONSENT_ASKED)
        }
    }


    fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }


    fun canUseInternet(): Boolean {
        val hasConsent = hasInternetConsent()
        val hasConnection = isInternetAvailable()

        return hasConsent && hasConnection
    }


    fun getConsentTimestamp(): Long {
        return sharedPreferences.getLong(KEY_CONSENT_TIMESTAMP, 0L)
    }
}