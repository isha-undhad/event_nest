
package com.example.eventnest

import com.example.eventnest.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun syncUserToFirestore(
        uid: String,
        name: String,
        email: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userDocRef = firestore.collection("users").document(uid)
            val snapshot = userDocRef.get().awaitTask()

            if (!snapshot.exists()) {
                val newUser = hashMapOf(
                    "uid" to uid,
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "role" to "user",
                    "createdAt" to System.currentTimeMillis()
                )
                userDocRef.set(newUser, SetOptions.merge()).awaitTask()
            } else {
                val updateData = mutableMapOf<String, Any>(
                    "uid" to uid
                )
                if (name.isNotBlank()) {
                    updateData["name"] = name.trim()
                }
                if (email.isNotBlank()) {
                    updateData["email"] = email.trim()
                }
                userDocRef.set(updateData, SetOptions.merge()).awaitTask()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRole(uid: String): String? = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users").document(uid).get().awaitTask()
            snapshot.getString("role")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUser(uid: String): User? = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users").document(uid).get().awaitTask()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users").get().awaitTask()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.apply {
                    if (uid.isBlank()) {
                        uid = doc.id
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Removes a user by deleting their document from the "users" collection in Firestore.
     *
     * NOTE: Actually disabling or deleting another user's Firebase Auth account requires
     * the Firebase Admin SDK (server-side / Cloud Function) since client SDKs cannot delete
     * another user's Auth account directly. Removing their Firestore profile document cuts
     * off their profile and role-based access in-app.
     */
    suspend fun removeUser(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(uid.isNotBlank()) { "User UID cannot be blank" }
            firestore.collection("users").document(uid).delete().awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time listener on the "users" collection for administrative monitoring.
     */
    fun listenToAllUsers(
        onUpdate: (List<User>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration {
        return firestore.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError?.invoke(error)
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(User::class.java)?.apply {
                    if (uid.isBlank()) {
                        uid = doc.id
                    }
                }
            }.orEmpty()
            onUpdate(users)
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                continuation.resume(result)
            }
            addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
            addOnCanceledListener {
                continuation.cancel()
            }
        }
}
