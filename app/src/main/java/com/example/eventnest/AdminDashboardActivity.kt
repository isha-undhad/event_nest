package com.example.eventnest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.eventnest.repositories.AdminRepository
import com.example.eventnest.repositories.CategoryRepository
import com.example.eventnest.repositories.EventRepository
import com.example.eventnest.utils.formatCurrency
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class AdminDashboardActivity : BaseActivity() {

    private val adminRepository = AdminRepository()
    private val authRepository = AuthRepository()
    private val categoryRepository = CategoryRepository()
    private val eventRepository = EventRepository()

    // Views
    private lateinit var btnGoToHome: View
    private lateinit var btnPreviewAsUser: View
    private lateinit var btnLogoutAdmin: View
    private lateinit var tvStatRevenue: TextView
    private lateinit var tvStatTicketsSold: TextView
    private lateinit var tvStatActiveEvents: TextView
    private lateinit var tvStatUpcomingEvents: TextView

    private lateinit var cardActionAddEvent: View
    private lateinit var cardActionManageEvents: View
    private lateinit var cardActionViewBookings: View
    private lateinit var cardActionManageUsers: View
    private lateinit var cardActionSeedData: View

    // Firestore real-time listener registrations
    private var bookingStatsListener: ListenerRegistration? = null
    private var eventStatsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnGoToHome = findViewById(R.id.btnGoToHome)
        btnPreviewAsUser = findViewById(R.id.btnPreviewAsUser)
        btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin)
        tvStatRevenue = findViewById(R.id.tvStatRevenue)
        tvStatTicketsSold = findViewById(R.id.tvStatTicketsSold)
        tvStatActiveEvents = findViewById(R.id.tvStatActiveEvents)
        tvStatUpcomingEvents = findViewById(R.id.tvStatUpcomingEvents)

        cardActionAddEvent = findViewById(R.id.cardActionAddEvent)
        cardActionManageEvents = findViewById(R.id.cardActionManageEvents)
        cardActionViewBookings = findViewById(R.id.cardActionViewBookings)
        cardActionManageUsers = findViewById(R.id.cardActionManageUsers)
        cardActionSeedData = findViewById(R.id.cardActionSeedData)

        // Set initial placeholder states
        tvStatRevenue.text = formatCurrency(0.0)
        tvStatTicketsSold.text = "0"
        tvStatActiveEvents.text = "0"
        tvStatUpcomingEvents.text = "0"
    }

    private fun setupListeners() {
        btnGoToHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        btnPreviewAsUser.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("isAdminPreview", true)
            }
            startActivity(intent)
        }

        btnLogoutAdmin.setOnClickListener {
            btnLogoutAdmin.isEnabled = false
            lifecycleScope.launch {
                authRepository.signOut(this@AdminDashboardActivity)
                val intent = Intent(this@AdminDashboardActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }

        cardActionAddEvent.setOnClickListener {
            startActivity(Intent(this, AddEditEventActivity::class.java))
        }

        cardActionManageEvents.setOnClickListener {
            startActivity(Intent(this, ManageEventsActivity::class.java))
        }

        cardActionViewBookings.setOnClickListener {
            startActivity(Intent(this, AdminBookingsActivity::class.java))
        }

        cardActionManageUsers.setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }

        cardActionSeedData.setOnClickListener {
            seedData()
        }
    }

    override fun onStart() {
        super.onStart()
        startRealtimeStatsListeners()
    }

    override fun onStop() {
        super.onStop()
        stopRealtimeStatsListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRealtimeStatsListeners()
    }

    /**
     * Attaches live snapshot listeners to Firestore collections to compute metrics in real-time.
     */
    private fun startRealtimeStatsListeners() {
        // Remove any existing listeners first to prevent duplicates
        stopRealtimeStatsListeners()

        // 1. Listen for confirmed bookings to calculate Total Revenue and Tickets Sold
        bookingStatsListener = adminRepository.listenToBookingStats { totalRevenue, ticketsSold ->
            tvStatRevenue.text = formatCurrency(totalRevenue)
            tvStatTicketsSold.text = ticketsSold.toString()
        }

        // 2. Listen for active events to calculate Active Events and Upcoming Events
        eventStatsListener = adminRepository.listenToEventStats { activeCount, upcomingCount ->
            tvStatActiveEvents.text = activeCount.toString()
            tvStatUpcomingEvents.text = upcomingCount.toString()
        }
    }

    /**
     * Detaches live snapshot listeners to prevent memory leaks and unnecessary network calls.
     */
    private fun stopRealtimeStatsListeners() {
        bookingStatsListener?.remove()
        bookingStatsListener = null

        eventStatsListener?.remove()
        eventStatsListener = null
    }

    private fun seedData() {
        cardActionSeedData.isEnabled = false
        Toast.makeText(this, "Seeding categories & 20 events...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            // Ensure default categories exist if Firestore was empty
            categoryRepository.seedDefaultCategoriesIfEmpty()

            val result = com.example.eventnest.debug.DebugEventSeeder.seed20Events()
            cardActionSeedData.isEnabled = true

            when (result) {
                is com.example.eventnest.debug.DebugEventSeeder.SeederResult.Success -> {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Successfully seeded ${result.writtenCount} events!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is com.example.eventnest.debug.DebugEventSeeder.SeederResult.MissingCategories -> {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Halted! Missing categories: ${result.missing.joinToString()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is com.example.eventnest.debug.DebugEventSeeder.SeederResult.Error -> {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Seeder error: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
