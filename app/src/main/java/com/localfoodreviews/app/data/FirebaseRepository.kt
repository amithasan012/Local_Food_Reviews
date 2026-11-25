package com.localfoodreviews.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

/** A deliberately small Firestore repository: no Cloud Functions and no paid APIs. */
class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun createAccount(
        rawUsername: String,
        rawEmail: String,
        password: String,
        onDone: (Result<UserProfile>) -> Unit
    ) {
        val username = rawUsername.trim()
        val usernameLowercase = username.lowercase()
        val email = rawEmail.trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    onDone(Result.failure(IllegalStateException("Could not create your account.")))
                    return@addOnSuccessListener
                }
                val profile = UserProfile(uid, username, usernameLowercase, email)
                val usernameRef = db.collection("usernames").document(usernameLowercase)
                val userRef = db.collection("users").document(uid)

                // Transaction makes the username check and reservation atomic.
                db.runTransaction { transaction ->
                    if (transaction.get(usernameRef).exists()) {
                        throw FirebaseFirestoreException(
                            "That username is already in use.",
                            FirebaseFirestoreException.Code.ABORTED
                        )
                    }
                    transaction.set(usernameRef, mapOf("uid" to uid, "username" to usernameLowercase))
                    transaction.set(
                        userRef,
                        mapOf(
                            "uid" to uid,
                            "username" to username,
                            "usernameLowercase" to usernameLowercase,
                            "email" to email,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    profile
                }.addOnSuccessListener { onDone(Result.success(it)) }
                    .addOnFailureListener { error ->
                        // Avoid leaving an Auth account behind when its username could not be reserved.
                        auth.currentUser?.delete()
                        auth.signOut()
                        onDone(Result.failure(error))
                    }
            }
            .addOnFailureListener { onDone(Result.failure(it)) }
    }

    fun addReview(
        restaurantName: String,
        cuisine: String,
        rating: Long,
        caption: String,
        photoBase64: String,
        onDone: (Result<Unit>) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onDone(Result.failure(IllegalStateException("Please sign in again.")))
            return
        }
        val userRef = db.collection("users").document(user.uid)
        userRef.get().addOnSuccessListener { userDoc ->
            val username = userDoc.getString("username")
            if (username.isNullOrBlank()) {
                onDone(Result.failure(IllegalStateException("Your profile is missing. Please sign out and create the account again.")))
                return@addOnSuccessListener
            }
            val reviewRef = db.collection("reviews").document()
            reviewRef.set(
                mapOf(
                    "uid" to user.uid,
                    "username" to username,
                    "restaurantName" to restaurantName.trim(),
                    "restaurantNameLowercase" to restaurantName.trim().lowercase(),
                    "cuisine" to cuisine.trim(),
                    "cuisineLowercase" to cuisine.trim().lowercase(),
                    "rating" to rating,
                    "caption" to caption.trim(),
                    "photoBase64" to photoBase64,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).addOnSuccessListener { onDone(Result.success(Unit)) }
                .addOnFailureListener { onDone(Result.failure(it)) }
        }.addOnFailureListener { onDone(Result.failure(it)) }
    }

    fun listenToFeed(onChanged: (List<Review>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return db.collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val reviews = snapshots?.documents?.mapNotNull { Review.fromDocument(it) }.orEmpty()
                onChanged(reviews)
            }
    }

    fun listenToMyReviews(
        uid: String,
        onChanged: (List<Review>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        // Sorting in the app avoids requiring a composite Firestore index for this student project.
        return db.collection("reviews")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val reviews = snapshots?.documents?.mapNotNull { Review.fromDocument(it) }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    .orEmpty()
                onChanged(reviews)
            }
    }

    fun updateReview(
        review: Review,
        restaurantName: String,
        cuisine: String,
        rating: Long,
        caption: String,
        photoBase64: String,
        onDone: (Result<Unit>) -> Unit
    ) {
        val fields = mapOf(
            "restaurantName" to restaurantName.trim(),
            "restaurantNameLowercase" to restaurantName.trim().lowercase(),
            "cuisine" to cuisine.trim(),
            "cuisineLowercase" to cuisine.trim().lowercase(),
            "rating" to rating,
            "caption" to caption.trim(),
            "photoBase64" to photoBase64,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("reviews").document(review.id)
            .set(fields, SetOptions.merge())
            .addOnSuccessListener { onDone(Result.success(Unit)) }
            .addOnFailureListener { onDone(Result.failure(it)) }
    }

    fun deleteReview(id: String, onDone: (Result<Unit>) -> Unit) {
        db.collection("reviews").document(id).delete()
            .addOnSuccessListener { onDone(Result.success(Unit)) }
            .addOnFailureListener { onDone(Result.failure(it)) }
    }
}
