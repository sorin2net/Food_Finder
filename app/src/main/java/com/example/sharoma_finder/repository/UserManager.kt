package com.example.sharoma_finder.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class UserManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveName(name: String) {
        sharedPreferences.edit { putString("user_name", name) }
    }

    fun getName(): String {
        return sharedPreferences.getString("user_name", "Utilizatorule") ?: "Utilizatorule"
    }


    fun savePoints(points: Int) {
        sharedPreferences.edit { putInt("user_points", points) }
    }
    fun saveLastTimerTimestamp(timestamp: Long) {
        sharedPreferences.edit { putLong("last_timer_timestamp", timestamp) }
    }

    fun getLastTimerTimestamp(): Long {
        return sharedPreferences.getLong("last_timer_timestamp", 0L)
    }

    fun getPoints(): Int {
        return sharedPreferences.getInt("user_points", 0)
    }

    fun saveImagePath(path: String) {
        sharedPreferences.edit { putString("user_image_path", path) }
    }

     fun getImagePath(): String? {
        return sharedPreferences.getString("user_image_path", null)
    }

    fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val fileName = "profile_picture.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}