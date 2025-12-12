package com.example.sharoma_finder.domain

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

@Keep
@Entity(tableName = "stores") // ✅ 1. Definim tabela pentru Room
data class StoreModel(
    // ✅ 2. Cheia primară (am mutat-o la început pentru claritate)
    @PrimaryKey
    var firebaseKey: String = "",

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

    // Room va folosi Converter-ul creat anterior pentru a salva această Listă
    var Tags: List<String> = emptyList()
) : Serializable {

    // ✅ 3. Ignorăm acest câmp la salvarea în baza de date
    // (el se calculează live pe baza GPS-ului, nu e o dată fixă a magazinului)
    @Ignore
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

    // Funcție pentru a verifica dacă restaurantul are un anumit tag
    fun hasTag(tagName: String): Boolean {
        return Tags.any { it.equals(tagName, ignoreCase = true) }
    }

    // Funcție pentru a verifica dacă are cel puțin unul din tag-urile date
    fun hasAnyTag(tagNames: List<String>): Boolean {
        return Tags.any { tag ->
            tagNames.any { it.equals(tag, ignoreCase = true) }
        }
    }
}