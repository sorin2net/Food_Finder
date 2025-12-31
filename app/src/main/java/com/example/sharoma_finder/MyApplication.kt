package com.example.sharoma_finder

import android.app.Application
import android.util.Log
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics


class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()


        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
        }

        try {
            val crashlytics = FirebaseCrashlytics.getInstance()

            crashlytics.apply {
                setCrashlyticsCollectionEnabled(true)

                setUserId("anonymous_user")

                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("debug_mode", BuildConfig.DEBUG)
            }

        } catch (e: Exception) {
        }
    }
}