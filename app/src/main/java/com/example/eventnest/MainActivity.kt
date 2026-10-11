package com.example.eventnest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeFromGate(this, authRepository, userRepository)
    }

    companion object {
        private const val TAG = "MainActivity"

        /**
         * Reusable gatekeeping routing logic:
         * 1. If not logged in -> navigate to LoginActivity
         * 2. If logged in -> inspect role: admin -> AdminDashboardActivity, user -> HomeActivity
         * 3. Finishes the source activity so it's not kept on the back stack.
         */
        fun routeFromGate(
            activity: AppCompatActivity,
            authRepository: AuthRepository = AuthRepository(),
            userRepository: UserRepository = UserRepository()
        ) {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                val intent = Intent(activity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                activity.startActivity(intent)
                activity.finish()
                return
            }

            activity.lifecycleScope.launch {
                try {
                    val firestoreUser = userRepository.getUser(currentUser.uid)
                    val role = firestoreUser?.role ?: userRepository.getUserRole(currentUser.uid) ?: "user"
                    val isAdmin = role.equals("admin", ignoreCase = true)

                    Log.d(TAG, "Routing authenticated user (${currentUser.uid}) with role '$role' (isAdmin=$isAdmin)")

                    val targetClass = if (isAdmin) {
                        AdminDashboardActivity::class.java
                    } else {
                        HomeActivity::class.java
                    }

                    val intent = Intent(activity, targetClass).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    activity.startActivity(intent)
                    activity.finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error resolving user role, defaulting to HomeActivity: ${e.message}", e)
                    val fallbackIntent = Intent(activity, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    activity.startActivity(fallbackIntent)
                    activity.finish()
                }
            }
        }
    }
}