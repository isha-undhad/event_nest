package com.example.eventnest

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventnest.adapters.ManageUserAdapter
import com.example.eventnest.models.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ManageUsersActivity : BaseActivity() {

    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    // Views
    private lateinit var btnBackManageUsers: View
    private lateinit var progressManageUsers: ProgressBar
    private lateinit var rvManageUsers: RecyclerView
    private lateinit var layoutManageUsersEmpty: View

    private lateinit var adapter: ManageUserAdapter
    private var usersListener: ListenerRegistration? = null
    private var allUsers: List<User> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manageUsersRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupRecyclerView()
        setupListeners()
    }

    private fun initViews() {
        btnBackManageUsers = findViewById(R.id.btnBackManageUsers)
        progressManageUsers = findViewById(R.id.progressManageUsers)
        rvManageUsers = findViewById(R.id.rvManageUsers)
        layoutManageUsersEmpty = findViewById(R.id.layoutManageUsersEmpty)
    }

    private fun setupRecyclerView() {
        val currentAdminUid = authRepository.currentUser?.uid.orEmpty()
        adapter = ManageUserAdapter(
            currentAdminUid = currentAdminUid,
            onRemoveClick = { user ->
                showRemoveUserConfirmation(user)
            }
        )
        rvManageUsers.layoutManager = LinearLayoutManager(this)
        rvManageUsers.adapter = adapter
    }

    private fun setupListeners() {
        btnBackManageUsers.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        startUsersListener()
    }

    override fun onStop() {
        super.onStop()
        stopUsersListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUsersListener()
    }

    private fun startUsersListener() {
        stopUsersListener()

        if (allUsers.isEmpty()) {
            progressManageUsers.visibility = View.VISIBLE
        }

        usersListener = userRepository.listenToAllUsers(
            onUpdate = { users ->
                progressManageUsers.visibility = View.GONE
                allUsers = users
                adapter.submitList(users)

                if (users.isEmpty()) {
                    rvManageUsers.visibility = View.GONE
                    layoutManageUsersEmpty.visibility = View.VISIBLE
                } else {
                    rvManageUsers.visibility = View.VISIBLE
                    layoutManageUsersEmpty.visibility = View.GONE
                }
            },
            onError = { error ->
                progressManageUsers.visibility = View.GONE
                Toast.makeText(this, "Error loading users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun stopUsersListener() {
        usersListener?.remove()
        usersListener = null
    }

    /**
     * Shows a confirmation dialog before removing the user from Firestore.
     */
    private fun showRemoveUserConfirmation(user: User) {
        val displayName = when {
            user.name.isNotBlank() -> user.name
            user.email.isNotBlank() -> user.email
            else -> "this user"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Remove User")
            .setMessage("Remove this user? This cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                removeUser(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Removes the user document from Firestore.
     *
     * NOTE: Actually disabling or deleting another user's Firebase Auth account requires
     * the Firebase Admin SDK (server-side / Cloud Function) since client SDKs cannot delete
     * another user's Auth account directly. Removing their Firestore profile document cuts
     * off their profile and role-based access in-app.
     */
    private fun removeUser(user: User) {
        lifecycleScope.launch {
            val result = userRepository.removeUser(user.uid)
            if (result.isSuccess) {
                Toast.makeText(this@ManageUsersActivity, "User removed successfully", Toast.LENGTH_SHORT).show()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to remove user"
                Toast.makeText(this@ManageUsersActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
