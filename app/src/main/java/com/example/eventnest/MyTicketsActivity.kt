package com.example.eventnest

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventnest.models.Booking
import com.example.eventnest.repositories.BookingRepository
import com.example.eventnest.utils.NavigationUtils
import com.example.eventnest.utils.formatCurrency
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class MyTicketsActivity : BaseActivity() {

    private val authRepository = AuthRepository()
    private val bookingRepository = BookingRepository()

    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var rvBookings: RecyclerView
    private lateinit var progressBarTickets: ProgressBar
    private lateinit var llTicketsEmpty: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var btnBrowseEvents: Button
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var adapter: BookingAdapter

    private var allBookings: List<Booking> = emptyList()
    private var currentFilter: String = "all"
    private var isAdminPreview: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_tickets)

        isAdminPreview = intent.getBooleanExtra("isAdminPreview", false)

        initViews()
        NavigationUtils.setupBottomNavInsets(findViewById(R.id.ticketsRoot), bottomNav)
        setupBottomNav()
        setupFilters()
        setupRecyclerView()
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
        bottomNav.selectedItemId = R.id.action_my_tickets
        loadUserBookings()
    }

    private fun initViews() {
        chipGroupFilters = findViewById(R.id.chipGroupFilters)
        rvBookings = findViewById(R.id.rvBookings)
        progressBarTickets = findViewById(R.id.progressBarTickets)
        llTicketsEmpty = findViewById(R.id.llTicketsEmpty)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        btnBrowseEvents = findViewById(R.id.btnBrowseEvents)
        bottomNav = findViewById(R.id.bottomNav)

        findViewById<View>(R.id.incAdminPreviewBanner).visibility = if (isAdminPreview) View.VISIBLE else View.GONE
        setupExitAdminPreview()

        btnBrowseEvents.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("isAdminPreview", isAdminPreview)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupExitAdminPreview() {
        val banner = findViewById<View?>(R.id.incAdminPreviewBanner)
        banner?.findViewById<View>(R.id.btnExitAdminPreview)?.setOnClickListener {
            finish()
        }
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.action_my_tickets
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
                R.id.action_my_tickets -> true
                R.id.action_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("isAdminPreview", isAdminPreview)
                    })
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFilters() {
        chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipAll
            currentFilter = when (checkedId) {
                R.id.chipConfirmed -> "confirmed"
                R.id.chipPending -> "pending"
                R.id.chipCancelled -> "cancelled"
                else -> "all"
            }
            applyCurrentFilter()
        }
    }

    private fun setupRecyclerView() {
        adapter = BookingAdapter { booking ->
            val intent = Intent(this, BookingConfirmationActivity::class.java).apply {
                putExtra("bookingId", booking.bookingId)
                putExtra("isReadOnly", true)
            }
            startActivity(intent)
        }
        rvBookings.layoutManager = LinearLayoutManager(this)
        rvBookings.adapter = adapter
    }

    private fun loadUserBookings() {
        val user = authRepository.currentUser
        val userId = user?.uid ?: "guest_user"

        progressBarTickets.visibility = View.VISIBLE
        llTicketsEmpty.visibility = View.GONE

        lifecycleScope.launch {
            val bookings = bookingRepository.getUserBookings(userId)
            progressBarTickets.visibility = View.GONE
            allBookings = bookings
            applyCurrentFilter()
        }
    }

    private fun applyCurrentFilter() {
        val filtered = when (currentFilter) {
            "all" -> allBookings
            else -> allBookings.filter { it.status.equals(currentFilter, ignoreCase = true) }
        }

        if (filtered.isEmpty()) {
            llTicketsEmpty.visibility = View.VISIBLE
            rvBookings.visibility = View.GONE
            tvEmptyTitle.text = if (allBookings.isEmpty()) {
                "You haven't booked any tickets yet"
            } else {
                "No ${currentFilter.replaceFirstChar { it.uppercase() }} tickets found"
            }
        } else {
            llTicketsEmpty.visibility = View.GONE
            rvBookings.visibility = View.VISIBLE
            adapter.submitList(filtered)
        }
    }

    private class BookingAdapter(
        private val onItemClick: (Booking) -> Unit
    ) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

        private var items: List<Booking> = emptyList()

        fun submitList(newItems: List<Booking>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking_card, parent, false)
            return BookingViewHolder(view, onItemClick)
        }

        override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class BookingViewHolder(
            itemView: View,
            private val onItemClick: (Booking) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {

            private val cardBookingItem: View = itemView.findViewById(R.id.cardBookingItem)
            private val tvEventName: TextView = itemView.findViewById(R.id.tvBookingEventName)
            private val tvStatus: TextView = itemView.findViewById(R.id.tvBookingStatus)
            private val tvDate: TextView = itemView.findViewById(R.id.tvBookingDate)
            private val tvTickets: TextView = itemView.findViewById(R.id.tvTicketCount)
            private val tvTotal: TextView = itemView.findViewById(R.id.tvBookingTotal)

            fun bind(booking: Booking) {
                tvEventName.text = booking.eventName
                tvDate.text = if (booking.bookingDate.isNotBlank()) booking.bookingDate else "Date pending"
                tvTickets.text = if (booking.numberOfTickets == 1) "1 Ticket" else "${booking.numberOfTickets} Tickets"
                tvTotal.text = formatCurrency(booking.totalAmount)

                val context = itemView.context
                val statusLower = booking.status.lowercase()

                when (statusLower) {
                    "confirmed" -> {
                        tvStatus.text = "Confirmed"
                        tvStatus.setBackgroundResource(R.drawable.bg_badge_success)
                        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
                        // Reset strikethrough & restore primary color
                        tvTotal.paintFlags = tvTotal.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        tvTotal.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    }
                    "pending" -> {
                        tvStatus.text = "Pending"
                        tvStatus.setBackgroundResource(R.drawable.bg_badge_warning)
                        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning))
                        // Reset strikethrough & restore primary color
                        tvTotal.paintFlags = tvTotal.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        tvTotal.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    }
                    "cancelled" -> {
                        tvStatus.text = "Cancelled"
                        tvStatus.setBackgroundResource(R.drawable.bg_badge_muted)
                        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        // Apply strikethrough and muted grey on total amount
                        tvTotal.paintFlags = tvTotal.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        tvTotal.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    }
                    else -> {
                        tvStatus.text = booking.status.ifBlank { "Confirmed" }
                        tvStatus.setBackgroundResource(R.drawable.bg_badge_success)
                        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
                        tvTotal.paintFlags = tvTotal.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        tvTotal.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    }
                }

                cardBookingItem.setOnClickListener {
                    onItemClick(booking)
                }
            }
        }
    }
}
