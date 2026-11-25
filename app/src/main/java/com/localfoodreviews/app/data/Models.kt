package com.localfoodreviews.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class UserProfile(
    val uid: String,
    val username: String,
    val usernameLowercase: String,
    val email: String
)

data class Review(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val restaurantName: String = "",
    val restaurantNameLowercase: String = "",
    val cuisine: String = "",
    val cuisineLowercase: String = "",
    val rating: Long = 0L,
    val caption: String = "",
    val photoBase64: String = "",
    val createdAt: Timestamp? = null
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): Review? {
            val uid = document.getString("uid") ?: return null
            return Review(
                id = document.id,
                uid = uid,
                username = document.getString("username") ?: "food lover",
                restaurantName = document.getString("restaurantName") ?: "",
                restaurantNameLowercase = document.getString("restaurantNameLowercase") ?: "",
                cuisine = document.getString("cuisine") ?: "",
                cuisineLowercase = document.getString("cuisineLowercase") ?: "",
                rating = document.getLong("rating") ?: 0L,
                caption = document.getString("caption") ?: "",
                photoBase64 = document.getString("photoBase64") ?: "",
                createdAt = document.getTimestamp("createdAt")
            )
        }
    }
}
