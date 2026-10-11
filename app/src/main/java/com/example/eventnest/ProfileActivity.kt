package com.example.eventnest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.eventnest.utils.NavigationUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private lateinit var tvProfileInitials: TextView
    private lateinit var ivProfileAvatar: ImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileRole: TextView
    private lateinit var rowMyTickets: View
    private lateinit var rowAdminDashboard: View
    private lateinit var dividerAdmin: View
    private lateinit var rowSignOut: View
    private lateinit var bottomNav: BottomNavigationView
    private var isAdminPreview: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        isAdminPreview = intent.getBooleanExtra("isAdminPreview", false)

        initViews()
        NavigationUtils.setupBottomNavInsets(findViewById(R.id.profileRoot), bottomNav)
        setupBottomNav()
        setupListeners()
        loadUserData()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        isAdminPreview = intent.getBooleanExtra("isAdminPreview", false)
        findViewById<View?>(R.id.incAdminPreviewBanner)?.visibility = if (isAdminPreview) View.VISIBLE else View.GONE
        setupExitAdminPreview()
    }

    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.action_profile
    }

    private fun initViews() {
        tvProfileInitials = findViewById(R.id.tvProfileInitials)
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfileRole = findViewById(R.id.tvProfileRole)
        rowMyTickets = findViewById(R.id.rowMyTickets)
        rowAdminDashboard = findViewById(R.id.rowAdminDashboard)
        dividerAdmin = findViewById(R.id.dividerAdmin)
        rowSignOut = findViewById(R.id.rowSignOut)
        bottomNav = findViewById(R.id.bottomNav)

        findViewById<View>(R.id.incAdminPreviewBanner).visibility = if (isAdminPreview) View.VISIBLE else View.GONE
        setupExitAdminPreview()
    }

    private fun setupExitAdminPreview() {
        val banner = findViewById<View?>(R.id.incAdminPreviewBanner)
        banner?.findViewById<View>(R.id.btnExitAdminPreview)?.setOnClickListener {
            finish()
        }
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.action_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    startActivity(Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("isAdminPreview", isAdminPreview)
                    })
                    finish()
                    true
                }
                R.id.action_my_tickets -> {
                    startActivity(Intent(this, MyTicketsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("isAdminPreview", isAdminPreview)
                    })
                    finish()
                    true
                }
                R.id.action_profile -> true
                else -> false
            }
        }
    }

    private fun setupListeners() {
        rowMyTickets.setOnClickListener {
            startActivity(Intent(this, MyTicketsActivity::class.java).apply {
                putExtra("isAdminPreview", isAdminPreview)
            })
        }

        rowAdminDashboard.setOnClickListener {
            val intent = Intent(this, AdminDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }

        rowSignOut.setOnClickListener {
            rowSignOut.isEnabled = false
            lifecycleScope.launch {
                authRepository.signOut(this@ProfileActivity)
                val intent = Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun loadUserData() {
        val user = authRepository.currentUser ?: run {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        // Initial preview from FirebaseUser
        val defaultName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
            ?: "User"
        tvProfileName.text = defaultName
        tvProfileEmail.text = user.email ?: "No email"
        tvProfileInitials.text = getInitials(defaultName)

        // Google Photo or initials
        val photoUrl = user.photoUrl
        if (photoUrl != null && photoUrl.toString().isNotBlank()) {
            ivProfileAvatar.visibility = View.VISIBLE
            tvProfileInitials.visibility = View.GONE
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .into(ivProfileAvatar)
        } else {
            ivProfileAvatar.visibility = View.GONE
            tvProfileInitials.visibility = View.VISIBLE
        }

        // Fetch user data and role from UserRepository (same way MainActivity does)
        lifecycleScope.launch {
            val firestoreUser = userRepository.getUser(user.uid)
            val role = firestoreUser?.role ?: userRepository.getUserRole(user.uid) ?: "user"
            tvProfileRole.text = role.uppercase()

            if (firestoreUser != null && firestoreUser.name.isNotBlank()) {
                tvProfileName.text = firestoreUser.name
                if (photoUrl == null) {
                    tvProfileInitials.text = getInitials(firestoreUser.name)
                }
            }

            if (firestoreUser != null && firestoreUser.email.isNotBlank()) {
                tvProfileEmail.text = firestoreUser.email
            }

            // If user's role == "admin", show extra row above Sign Out: "Admin Dashboard"
            val isAdmin = role.equals("admin", ignoreCase = true)
            rowAdminDashboard.visibility = if (isAdmin) View.VISIBLE else View.GONE
            dividerAdmin.visibility = if (isAdmin) View.VISIBLE else View.GONE
        }
    }

    companion object {
        fun getInitials(name: String): String {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return "U"
            val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
                parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
                parts.size == 1 -> parts[0].take(1).uppercase()
                else -> "U"
            }
        }
    }
}
