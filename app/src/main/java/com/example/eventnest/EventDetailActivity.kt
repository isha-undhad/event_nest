package com.example.eventnest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.eventnest.models.Event
import com.example.eventnest.repositories.CategoryRepository
import com.example.eventnest.repositories.EventRepository
import com.example.eventnest.utils.formatCurrency
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class EventDetailActivity : BaseActivity() {

    private val eventRepository = EventRepository()
    private val categoryRepository = CategoryRepository()

    // Views
    private lateinit var ivDetailBanner: ImageView
    private lateinit var btnBack: View
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailTime: TextView
    private lateinit var tvDetailVenue: TextView
    private lateinit var tvDetailPriceRow: TextView
    private lateinit var tvDetailSeatsRow: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var tvDetailBottomPrice: TextView
    private lateinit var btnBookTickets: MaterialButton

    private lateinit var scrollViewContent: View
    private lateinit var cardBottomBar: View
    private lateinit var loadingProgress: ProgressBar
    private lateinit var layoutError: View
    private lateinit var btnRetry: MaterialButton

    private var eventId: String = ""
    private var loadedEvent: Event? = null
    private var isAdminPreview: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        isAdminPreview = intent.getBooleanExtra("isAdminPreview", false)

        val banner = findViewById<View>(R.id.incAdminPreviewBanner)
        banner.visibility = if (isAdminPreview) View.VISIBLE else View.GONE
        setupExitAdminPreview()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailCoordinator)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            if (isAdminPreview) {
                banner.setPadding(
                    banner.paddingLeft,
                    systemBars.top + (7 * resources.displayMetrics.density).toInt(),
                    banner.paddingRight,
                    (7 * resources.displayMetrics.density).toInt()
                )
            }
            insets
        }

        eventId = intent.getStringExtra("eventId")
            ?: intent.getStringExtra(EXTRA_EVENT_ID)
            .orEmpty()

        initViews()
        setupListeners()
        loadEventFromFirestore()
    }

    private fun initViews() {
        ivDetailBanner = findViewById(R.id.ivDetailBanner)
        btnBack = findViewById(R.id.btnBack)
        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailTitle = findViewById(R.id.tvDetailTitle)
        tvDetailDate = findViewById(R.id.tvDetailDate)
        tvDetailTime = findViewById(R.id.tvDetailTime)
        tvDetailVenue = findViewById(R.id.tvDetailVenue)
        tvDetailPriceRow = findViewById(R.id.tvDetailPriceRow)
        tvDetailSeatsRow = findViewById(R.id.tvDetailSeatsRow)
        tvDetailDescription = findViewById(R.id.tvDetailDescription)
        tvDetailBottomPrice = findViewById(R.id.tvDetailBottomPrice)
        btnBookTickets = findViewById(R.id.btnBookTickets)

        scrollViewContent = findViewById(R.id.scrollViewContent)
        cardBottomBar = findViewById(R.id.cardBottomBar)
        loadingProgress = findViewById(R.id.loadingProgress)
        layoutError = findViewById(R.id.layoutError)
        btnRetry = findViewById(R.id.btnRetry)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnRetry.setOnClickListener {
            loadEventFromFirestore()
        }

        btnBookTickets.setOnClickListener {
            val event = loadedEvent ?: return@setOnClickListener
            if (event.totalAvailableSeats > 0) {
                val intent = Intent(this, BookTicketsActivity::class.java).apply {
                    putExtra("eventId", event.eventId)
                    putExtra(EXTRA_EVENT_ID, event.eventId)
                    putExtra("isAdminPreview", isAdminPreview)
                }
                startActivity(intent)
            }
        }
    }

    private fun setupExitAdminPreview() {
        val banner = findViewById<View?>(R.id.incAdminPreviewBanner)
        banner?.findViewById<View>(R.id.btnExitAdminPreview)?.setOnClickListener {
            finish()
        }
    }

    private fun loadEventFromFirestore() {
        if (eventId.isBlank()) {
            showErrorState()
            return
        }

        showLoadingState()

        lifecycleScope.launch {
            val result = eventRepository.getEventById(eventId)
            result.onSuccess { event ->
                if (event != null) {
                    loadedEvent = event
                    resolveCategoryAndPopulate(event)
                    showContentState()
                } else {
                    showErrorState()
                }
            }.onFailure {
                showErrorState()
            }
        }
    }

    private fun resolveCategoryAndPopulate(event: Event) {
        populateEventDetails(event, event.categoryId)

        // Resolve human-readable category name if categoryId is an ID
        lifecycleScope.launch {
            val categoryResult = categoryRepository.getCategoryById(event.categoryId)
            categoryResult.onSuccess { category ->
                if (category != null && category.categoryName.isNotBlank()) {
                    tvDetailCategory.text = category.categoryName.uppercase()
                }
            }
        }
    }

    private fun populateEventDetails(event: Event, fallbackCategoryName: String) {
        // Banner Image loaded with Glide
        if (event.bannerImageUrl.isNotBlank()) {
            Glide.with(this)
                .load(event.bannerImageUrl)
                .placeholder(R.drawable.ic_event_placeholder)
                .error(R.drawable.ic_event_placeholder)
                .centerCrop()
                .into(ivDetailBanner)
        } else {
            ivDetailBanner.setImageResource(R.drawable.ic_event_placeholder)
        }

        // Title and Category
        tvDetailTitle.text = event.eventName
        tvDetailCategory.text = fallbackCategoryName.uppercase()

        // Date, Time, Venue
        tvDetailDate.text = event.date.ifBlank { "Date TBA" }
        tvDetailTime.text = event.time.ifBlank { "Time TBA" }
        tvDetailVenue.text = event.venue.ifBlank { "Venue TBA" }

        // Price formatted via formatCurrency() in ₹
        val minPrice = event.minPrice
        val maxPrice = event.maxPrice
        val formattedPrice = when {
            minPrice <= 0.0 -> getString(R.string.price_free)
            minPrice == maxPrice -> formatCurrency(minPrice)
            else -> "From ${formatCurrency(minPrice)}"
        }
        tvDetailPriceRow.text = formattedPrice
        tvDetailBottomPrice.text = formattedPrice

        // Seats available row with Warning/Sold-out coloring rules
        val total = event.totalCapacitySeats
        val available = event.totalAvailableSeats
        when {
            available <= 0 -> {
                tvDetailSeatsRow.text = "0 / $total (Sold out)"
                tvDetailSeatsRow.setBackgroundResource(R.drawable.bg_badge_error)
                tvDetailSeatsRow.setTextColor(ContextCompat.getColor(this, R.color.error))

                // Disabled Book Tickets Button (Grey, "Sold Out")
                btnBookTickets.isEnabled = false
                btnBookTickets.text = getString(R.string.sold_out)
                btnBookTickets.setBackgroundColor(ContextCompat.getColor(this, R.color.text_muted))
            }
            available < 10 -> {
                tvDetailSeatsRow.text = "$available / $total seats left"
                tvDetailSeatsRow.setBackgroundResource(R.drawable.bg_badge_warning)
                tvDetailSeatsRow.setTextColor(ContextCompat.getColor(this, R.color.warning))

                btnBookTickets.isEnabled = true
                btnBookTickets.text = getString(R.string.book_tickets)
                btnBookTickets.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            }
            else -> {
                tvDetailSeatsRow.text = "$available / $total seats left"
                tvDetailSeatsRow.setBackgroundResource(R.drawable.bg_badge_seats)
                tvDetailSeatsRow.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

                btnBookTickets.isEnabled = true
                btnBookTickets.text = getString(R.string.book_tickets)
                btnBookTickets.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            }
        }

        // Description text block
        tvDetailDescription.text = event.description.ifBlank { "No event description provided." }
    }

    private fun showLoadingState() {
        loadingProgress.visibility = View.VISIBLE
        layoutError.visibility = View.GONE
        scrollViewContent.visibility = View.INVISIBLE
        cardBottomBar.visibility = View.GONE
    }

    private fun showContentState() {
        loadingProgress.visibility = View.GONE
        layoutError.visibility = View.GONE
        scrollViewContent.visibility = View.VISIBLE
        cardBottomBar.visibility = View.VISIBLE
    }

    private fun showErrorState() {
        loadingProgress.visibility = View.GONE
        layoutError.visibility = View.VISIBLE
        scrollViewContent.visibility = View.INVISIBLE
        cardBottomBar.visibility = View.GONE
    }

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_NAME = "extra_event_name"
        const val EXTRA_EVENT_CATEGORY = "extra_event_category"
        const val EXTRA_EVENT_VENUE = "extra_event_venue"
        const val EXTRA_EVENT_DATE = "extra_event_date"
        const val EXTRA_EVENT_TIME = "extra_event_time"
        const val EXTRA_EVENT_PRICE = "extra_event_price"
        const val EXTRA_EVENT_SEATS = "extra_event_seats"
        const val EXTRA_EVENT_DESCRIPTION = "extra_event_description"
        const val EXTRA_EVENT_BANNER = "extra_event_banner"
    }
}
