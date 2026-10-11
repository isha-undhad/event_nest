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
import com.example.eventnest.adapters.AdminBookingAdapter
import com.example.eventnest.models.Booking
import com.example.eventnest.repositories.BookingRepository
import com.example.eventnest.utils.AdminBookingsFilterHelper
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class AdminBookingsActivity : BaseActivity() {

    private val bookingRepository = BookingRepository()
    private val userRepository = UserRepository()

    // Views
    private lateinit var btnBackAdminBookings: View
    private lateinit var chipGroupBookingFilter: ChipGroup
    private lateinit var progressAdminBookings: ProgressBar
    private lateinit var rvAdminBookings: RecyclerView
    private lateinit var layoutAdminBookingsEmpty: View

    private lateinit var adapter: AdminBookingAdapter

    // State & real-time listener
    private var allBookings: List<Booking> = emptyList()
    private var currentFilter: String = AdminBookingsFilterHelper.FILTER_ALL
    private var bookingsListener: ListenerRegistration? = null

    // Cache of resolved customer user labels (userId -> Formatted Label)
    private val userLookupCache = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_bookings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminBookingsRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupRecyclerView()
        setupListeners()
    }

    private fun initViews() {
        btnBackAdminBookings = findViewById(R.id.btnBackAdminBookings)
        chipGroupBookingFilter = findViewById(R.id.chipGroupBookingFilter)
        progressAdminBookings = findViewById(R.id.progressAdminBookings)
        rvAdminBookings = findViewById(R.id.rvAdminBookings)
        layoutAdminBookingsEmpty = findViewById(R.id.layoutAdminBookingsEmpty)
    }

    private fun setupRecyclerView() {
        adapter = AdminBookingAdapter()
        rvAdminBookings.layoutManager = LinearLayoutManager(this)
        rvAdminBookings.adapter = adapter
    }

    private fun setupListeners() {
        btnBackAdminBookings.setOnClickListener {
            finish()
        }

        chipGroupBookingFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipBookingFilterAll
            currentFilter = when (checkedId) {
                R.id.chipBookingFilterConfirmed -> AdminBookingsFilterHelper.FILTER_CONFIRMED
                R.id.chipBookingFilterPending -> AdminBookingsFilterHelper.FILTER_PENDING
                R.id.chipBookingFilterCancelled -> AdminBookingsFilterHelper.FILTER_CANCELLED
                else -> AdminBookingsFilterHelper.FILTER_ALL
            }
            applyFilter()
        }
    }

    override fun onStart() {
        super.onStart()
        startBookingsListener()
    }

    override fun onStop() {
        super.onStop()
        stopBookingsListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBookingsListener()
    }

    /**
     * Attaches a real-time Firestore snapshot listener across the full "bookings" collection.
     */
    private fun startBookingsListener() {
        stopBookingsListener()

        if (allBookings.isEmpty()) {
            progressAdminBookings.visibility = View.VISIBLE
        }

        bookingsListener = bookingRepository.listenToAllBookings(
            onUpdate = { bookings ->
                progressAdminBookings.visibility = View.GONE
                allBookings = bookings
                applyFilter()
                resolveUserIdentities(bookings)
            },
            onError = { error ->
                progressAdminBookings.visibility = View.GONE
                Toast.makeText(this@AdminBookingsActivity, "Error loading bookings: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun stopBookingsListener() {
        bookingsListener?.remove()
        bookingsListener = null
    }

    private fun applyFilter() {
        val filtered = AdminBookingsFilterHelper.filterBookings(allBookings, currentFilter)
        adapter.submitList(filtered)

        if (filtered.isEmpty()) {
            rvAdminBookings.visibility = View.GONE
            layoutAdminBookingsEmpty.visibility = View.VISIBLE
        } else {
            rvAdminBookings.visibility = View.VISIBLE
            layoutAdminBookingsEmpty.visibility = View.GONE
        }
    }

    /**
     * Resolves unique user identities asynchronously and caches them to avoid redundant network calls.
     */
    private fun resolveUserIdentities(bookings: List<Booking>) {
        val uncachedUserIds = bookings
            .map { it.userId }
            .filter { it.isNotBlank() && !userLookupCache.containsKey(it) }
            .distinct()

        if (uncachedUserIds.isEmpty()) return

        lifecycleScope.launch {
            var updated = false
            for (uid in uncachedUserIds) {
                val user = userRepository.getUser(uid)
                val label = AdminBookingsFilterHelper.formatCustomerLabel(user?.name, user?.email, uid)
                userLookupCache[uid] = label
                updated = true
            }

            if (updated) {
                adapter.updateUserCache(userLookupCache)
            }
        }
    }
}
