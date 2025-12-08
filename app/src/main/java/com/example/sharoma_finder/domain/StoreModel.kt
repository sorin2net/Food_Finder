package com.example.sharoma_finder.domain

import java.io.Serializable

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

    // IMPORTANT: Cheie unică din Firebase pentru a diferenția magazinele
    var firebaseKey: String = ""
) : Serializable {
    // Funcție helper pentru a obține un ID unic complet
    fun getUniqueId(): String {
        return if (firebaseKey.isNotEmpty()) {
            firebaseKey
        } else {
            // Fallback: combină CategoryId cu Id pentru unicitate
            "${CategoryId}_${Id}"
        }
    }
}