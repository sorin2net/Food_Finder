package com.example.sharoma_finder.domain

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class StoreModel(
    var Id: Int = 0,
    var CategoryId: String = "",
    var Title: String = "",
    var Latitude: Double = 0.0,
    var Longitude: Double = 0.0,
    var Address: String = "",
    var Call: String = "",
    var Activity: String = "",
    var ShortAddress: String = "",
    var Hours: String = "",
    var ImagePath: String = "",
    var IsPopular: Boolean = false,
    var firebaseKey: String = "",

    // ✅ NOU: Câmp pentru tag-uri multiple
    var Tags: List<String> = emptyList()
) : Serializable {

    var distanceToUser: Float = -1f

    fun getUniqueId(): String {
        return if (firebaseKey.isNotEmpty()) {
            firebaseKey
        } else {
            "${CategoryId}_${Id}"
        }
    }

    fun isValid(): Boolean {
        return Title.isNotBlank() &&
                Latitude != 0.0 &&
                Longitude != 0.0 &&
                CategoryId.isNotBlank() &&
                Address.isNotBlank()
    }

    // ✅ NOU: Funcție pentru a verifica dacă restaurantul are un anumit tag
    fun hasTag(tagName: String): Boolean {
        return Tags.any { it.equals(tagName, ignoreCase = true) }
    }

    // ✅ NOU: Funcție pentru a verifica dacă are cel puțin unul din tag-urile date
    fun hasAnyTag(tagNames: List<String>): Boolean {
        return Tags.any { tag ->
            tagNames.any { it.equals(tag, ignoreCase = true) }
        }
    }
}