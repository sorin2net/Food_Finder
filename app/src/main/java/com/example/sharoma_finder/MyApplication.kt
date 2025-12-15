package com.example.sharoma_finder

import android.app.Application
import android.util.Log
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * ✅ Clasa Application - Prima componentă care se inițializează
 *
 * CÂND SE RULEAZĂ: Când aplicația pornește, ÎNAINTE de orice Activity
 * DE CE: Firebase Crashlytics trebuie inițializat GLOBAL, nu în fiecare Activity
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("MyApplication", "🚀 App starting - Initializing Firebase")

        // ✅ Inițializare Firebase (dacă nu e deja inițializat automat)
        try {
            FirebaseApp.initializeApp(this)
            Log.d("MyApplication", "✅ Firebase initialized")
        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Firebase init failed: ${e.message}")
        }

        // ✅ ACTIVEAZĂ Crashlytics (CRITIC!)
        // Fără asta, crash-urile NU vor fi raportate în Firebase Console
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true) // Activează raportarea

            // ✅ BONUS: Setează userId pentru debugging mai ușor
            // (Poți să-l schimbi când user-ul se loghează)
            setUserId("anonymous_user")

            // ✅ Adaugă custom keys pentru debugging
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("debug_mode", BuildConfig.DEBUG)

            Log.d("MyApplication", "✅ Crashlytics enabled and configured")
        }

        // ✅ TESTEAZĂ Crashlytics (doar în debug mode)
        if (BuildConfig.DEBUG) {
            // Uncomment asta pentru a testa că Crashlytics funcționează:
            // FirebaseCrashlytics.getInstance().log("Test crash log message")
        }
    }
}