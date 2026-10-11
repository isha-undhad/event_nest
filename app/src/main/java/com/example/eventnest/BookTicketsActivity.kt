package com.example.eventnest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.eventnest.models.Event
import com.example.eventnest.models.TicketTier
import com.example.eventnest.repositories.EventRepository
import com.example.eventnest.utils.formatCurrency
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class BookTicketsActivity : BaseActivity() {

    private val eventRepository = EventRepository()

    // Views
    private lateinit var btnBack: View
    private lateinit var ivEventThumbnail: ImageView
    private lateinit var tvEventName: TextView
    private lateinit var tvEventDateTime: TextView

    private lateinit var llSelectTiersContainer: LinearLayout

    private lateinit var btnMinus: MaterialButton
    private lateinit var tvQuantity: TextView
    private lateinit var btnPlus: MaterialButton
    private lateinit var tvCapWarning: TextView

    private lateinit var tvPricePerTicket: TextView
    private lateinit var tvQuantityMultiplier: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnProceedToPayment: MaterialButton
    private lateinit var tvPreviewPaymentNote: TextView
    private lateinit var progressBar: ProgressBar

    private var eventId: String = ""
    private var loadedEvent: Event? = null
    private var selectedTier: TicketTier? = null
    private var quantity: Int = 1
    private var isAdminPreview: Boolean = false

    private data class TierCardBinding(
        val tier: TicketTier,
        val cardView: MaterialCardView,
        val rbSelect: RadioButton
    )

    private val tierCardBindings = mutableListOf<TierCardBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_tickets)

        isAdminPreview = intent.getBooleanExtra("isAdminPreview", false)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bookTicketsRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val banner = findViewById<View>(R.id.incAdminPreviewBanner)
        banner.visibility = if (isAdminPreview) View.VISIBLE else View.GONE
        setupExitAdminPreview()

        eventId = intent.getStringExtra("eventId")
            ?: intent.getStringExtra(EventDetailActivity.EXTRA_EVENT_ID)
            .orEmpty()

        initViews()
        setupListeners()
        loadEventDetails()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBackBookTickets)
        ivEventThumbnail = findViewById(R.id.ivEventThumbnail)
        tvEventName = findViewById(R.id.tvBookEventName)
        tvEventDateTime = findViewById(R.id.tvBookEventDateTime)

        llSelectTiersContainer = findViewById(R.id.llSelectTiersContainer)

        btnMinus = findViewById(R.id.btnMinus)
        tvQuantity = findViewById(R.id.tvQuantity)
        btnPlus = findViewById(R.id.btnPlus)
        tvCapWarning = findViewById(R.id.tvCapWarning)

        tvPricePerTicket = findViewById(R.id.tvPricePerTicket)
        tvQuantityMultiplier = findViewById(R.id.tvQuantityMultiplier)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        btnProceedToPayment = findViewById(R.id.btnProceedToPayment)
        tvPreviewPaymentNote = findViewById(R.id.tvPreviewPaymentNote)
        progressBar = findViewById(R.id.bookProgressBar)

        if (isAdminPreview) {
            tvPreviewPaymentNote.visibility = View.VISIBLE
            btnProceedToPayment.isEnabled = false
            btnProceedToPayment.alpha = 0.5f
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // Stepper: Minus button (disabled at 1)
        btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateStepperAndPricing()
            }
        }

        // Stepper: Plus button (disabled when quantity == selectedTier.availableSeats)
        btnPlus.setOnClickListener {
            val tier = selectedTier ?: return@setOnClickListener
            if (quantity < tier.availableSeats) {
                quantity++
                updateStepperAndPricing()
            }
        }

        // Bottom sticky button: Proceed to Payment
        btnProceedToPayment.setOnClickListener {
            if (isAdminPreview) {
                Toast.makeText(this, "Payments are disabled in preview mode", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val event = loadedEvent ?: return@setOnClickListener
            val tier = selectedTier ?: run {
                Toast.makeText(this, "Please select a ticket tier", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (tier.availableSeats <= 0 || quantity < 1 || quantity > tier.availableSeats) {
                Toast.makeText(this, "Selected tier is not available for booking", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val totalAmount = quantity * tier.price

            val intent = Intent(this, PaymentMethodActivity::class.java).apply {
                putExtra("eventId", event.eventId)
                putExtra("eventName", event.eventName)
                putExtra("tierId", tier.tierId)
                putExtra("tierName", tier.tierName)
                putExtra("quantity", quantity)
                putExtra("totalAmount", totalAmount)
            }
            startActivity(intent)
        }
    }

    private fun setupExitAdminPreview() {
        val banner = findViewById<View?>(R.id.incAdminPreviewBanner)
        banner?.findViewById<View>(R.id.btnExitAdminPreview)?.setOnClickListener {
            finish()
        }
    }

    private fun loadEventDetails() {
        if (eventId.isBlank()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnProceedToPayment.isEnabled = false

        lifecycleScope.launch {
            val result = eventRepository.getEventById(eventId)
            progressBar.visibility = View.GONE

            result.onSuccess { event ->
                if (event == null) {
                    Toast.makeText(this@BookTicketsActivity, "Event details unavailable", Toast.LENGTH_SHORT).show()
                    finish()
                    return@onSuccess
                }

                loadedEvent = event
                populateEventCard(event)
                populateTierSelection(event)
                updateStepperAndPricing()
            }.onFailure { error ->
                Toast.makeText(this@BookTicketsActivity, "Failed to load event: ${error.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun populateEventCard(event: Event) {
        tvEventName.text = event.eventName
        tvEventDateTime.text = when {
            event.date.isNotBlank() && event.time.isNotBlank() -> "${event.date} • ${event.time}"
            event.date.isNotBlank() -> event.date
            else -> "Date & Time TBA"
        }

        // Small Thumbnail loaded with Glide
        if (event.bannerImageUrl.isNotBlank()) {
            Glide.with(this)
                .load(event.bannerImageUrl)
                .placeholder(R.drawable.ic_event_placeholder)
                .error(R.drawable.ic_event_placeholder)
                .centerCrop()
                .into(ivEventThumbnail)
        } else {
            ivEventThumbnail.setImageResource(R.drawable.ic_event_placeholder)
        }
    }

    private fun populateTierSelection(event: Event) {
        llSelectTiersContainer.removeAllViews()
        tierCardBindings.clear()
        selectedTier = null

        val tiers = event.effectiveTiers
        var firstAvailableTier: TicketTier? = null

        for (tier in tiers) {
            val view = layoutInflater.inflate(R.layout.item_tier_selectable, llSelectTiersContainer, false)
            val cardView = view.findViewById<MaterialCardView>(R.id.cardSelectableTier)
            val rbSelect = view.findViewById<RadioButton>(R.id.rbTierSelect)
            val tvName = view.findViewById<TextView>(R.id.tvSelectTierName)
            val tvSeats = view.findViewById<TextView>(R.id.tvSelectTierSeats)
            val tvPrice = view.findViewById<TextView>(R.id.tvSelectTierPrice)

            tvName.text = tier.tierName
            tvPrice.text = if (tier.price <= 0.0) getString(R.string.price_free) else formatCurrency(tier.price)

            val isSoldOut = tier.availableSeats <= 0
            if (isSoldOut) {
                tvSeats.text = getString(R.string.sold_out)
                tvSeats.setBackgroundResource(R.drawable.bg_badge_error)
                tvSeats.setTextColor(ContextCompat.getColor(this, R.color.error))
                cardView.alpha = 0.5f
                cardView.isClickable = false
            } else if (tier.availableSeats < 10) {
                tvSeats.text = "Only ${tier.availableSeats} seats left"
                tvSeats.setBackgroundResource(R.drawable.bg_badge_warning)
                tvSeats.setTextColor(ContextCompat.getColor(this, R.color.warning))
                cardView.alpha = 1.0f
                if (firstAvailableTier == null) firstAvailableTier = tier
            } else {
                tvSeats.text = "${tier.availableSeats} seats left"
                tvSeats.setBackgroundResource(R.drawable.bg_badge_seats)
                tvSeats.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                cardView.alpha = 1.0f
                if (firstAvailableTier == null) firstAvailableTier = tier
            }

            val binding = TierCardBinding(tier, cardView, rbSelect)
            tierCardBindings.add(binding)

            if (!isSoldOut) {
                cardView.setOnClickListener {
                    selectTier(binding)
                }
            }

            llSelectTiersContainer.addView(view)
        }

        // Auto-select first available tier
        if (firstAvailableTier != null) {
            val bindingToSelect = tierCardBindings.firstOrNull { it.tier.tierId == firstAvailableTier.tierId }
            if (bindingToSelect != null) {
                selectTier(bindingToSelect)
            }
        }
    }

    private fun selectTier(binding: TierCardBinding) {
        selectedTier = binding.tier

        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val strokeColor = ContextCompat.getColor(this, R.color.card_stroke)
        val strokeWidthSelected = dpToPx(2)
        val strokeWidthUnselected = dpToPx(1)

        for (b in tierCardBindings) {
            val isSelected = b.tier.tierId == binding.tier.tierId
            b.rbSelect.isChecked = isSelected
            b.cardView.strokeColor = if (isSelected) primaryColor else strokeColor
            b.cardView.strokeWidth = if (isSelected) strokeWidthSelected else strokeWidthUnselected
        }

        // Reset or clamp quantity
        quantity = 1
        updateStepperAndPricing()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun updateStepperAndPricing() {
        val tier = selectedTier

        if (tier == null || tier.availableSeats <= 0) {
            tvQuantity.text = "0"
            btnMinus.isEnabled = false
            btnPlus.isEnabled = false
            tvCapWarning.visibility = View.GONE

            tvPricePerTicket.text = "—"
            tvQuantityMultiplier.text = "× 0"
            tvTotalAmount.text = "₹0"

            btnProceedToPayment.isEnabled = false
            btnProceedToPayment.alpha = 0.5f
            return
        }

        // 1. Update Stepper Display
        quantity = quantity.coerceIn(1, tier.availableSeats)
        tvQuantity.text = quantity.toString()

        // Minus is disabled at 1
        btnMinus.isEnabled = quantity > 1

        // Plus is disabled when quantity == tier.availableSeats (never allow booking more than available)
        btnPlus.isEnabled = quantity < tier.availableSeats

        // Show note when quantity == tier.availableSeats or availableSeats < 10
        if (quantity == tier.availableSeats && tier.availableSeats > 0) {
            tvCapWarning.visibility = View.VISIBLE
            tvCapWarning.text = "Only ${tier.availableSeats} seats left in ${tier.tierName}"
        } else {
            tvCapWarning.visibility = View.GONE
        }

        // 2. Price Breakdown (strictly formatted in ₹ via formatCurrency())
        val unitPrice = tier.price
        val total = quantity * unitPrice

        tvPricePerTicket.text = if (unitPrice <= 0.0) getString(R.string.price_free) else formatCurrency(unitPrice)
        tvQuantityMultiplier.text = "× $quantity (${tier.tierName})"
        tvTotalAmount.text = if (unitPrice <= 0.0) getString(R.string.price_free) else formatCurrency(total)

        // Sticky button enabled state & preview mode enforcement
        if (isAdminPreview) {
            tvPreviewPaymentNote.visibility = View.VISIBLE
            btnProceedToPayment.isEnabled = false
            btnProceedToPayment.alpha = 0.5f
        } else {
            tvPreviewPaymentNote.visibility = View.GONE
            btnProceedToPayment.isEnabled = tier.availableSeats > 0 && quantity in 1..tier.availableSeats
            btnProceedToPayment.alpha = 1.0f
        }
    }
}
