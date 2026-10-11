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
import com.example.eventnest.repositories.BookingRepository
import com.example.eventnest.utils.formatCurrency
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UpiPaymentActivity : BaseActivity() {

    private val authRepository = AuthRepository()
    private val bookingRepository = BookingRepository()

    private lateinit var btnBackUpi: View
    private lateinit var tvUpiTotalAmount: TextView
    private lateinit var tvUpiEventSubtitle: TextView
    private lateinit var tilUpiId: TextInputLayout
    private lateinit var etUpiId: TextInputEditText
    private lateinit var btnPayUpiNow: MaterialButton
    private lateinit var btnProgressBar: CircularProgressIndicator

    private var eventId: String = ""
    private var eventName: String = ""
    private var tierId: String = ""
    private var tierName: String = ""
    private var quantity: Int = 1
    private var totalAmount: Double = 0.0

    // Basic UPI ID regex: something@something (e.g. ^[\w.\-]{2,}@[a-zA-Z]{2,}$)
    private val upiRegex = Regex("""^[\w.\-]{2,}@[a-zA-Z]{2,}$""")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upi_payment)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.upiPaymentRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        extractExtras()
        initViews()
        populateDetails()
        setupListeners()
    }

    private fun extractExtras() {
        eventId = intent.getStringExtra("eventId").orEmpty()
        eventName = intent.getStringExtra("eventName").orEmpty()
        tierId = intent.getStringExtra("tierId").orEmpty()
        tierName = intent.getStringExtra("tierName").orEmpty()
        quantity = intent.getIntExtra("quantity", 1)
        totalAmount = intent.getDoubleExtra("totalAmount", 0.0)
    }

    private fun initViews() {
        btnBackUpi = findViewById(R.id.btnBackUpi)
        tvUpiTotalAmount = findViewById(R.id.tvUpiTotalAmount)
        tvUpiEventSubtitle = findViewById(R.id.tvUpiEventSubtitle)
        tilUpiId = findViewById(R.id.tilUpiId)
        etUpiId = findViewById(R.id.etUpiId)
        btnPayUpiNow = findViewById(R.id.btnPayUpiNow)
        btnProgressBar = findViewById(R.id.btnProgressBar)
    }

    private fun populateDetails() {
        val formattedTotal = formatCurrency(totalAmount)
        tvUpiTotalAmount.text = formattedTotal
        val ticketText = if (quantity == 1) "1 Ticket" else "$quantity Tickets"
        val tierSnippet = if (tierName.isNotBlank()) " • $tierName" else ""
        tvUpiEventSubtitle.text = "$eventName$tierSnippet • $ticketText"
        btnPayUpiNow.text = "Pay $formattedTotal"
    }

    private fun setupListeners() {
        btnBackUpi.setOnClickListener {
            finish()
        }

        btnPayUpiNow.setOnClickListener {
            processUpiPayment()
        }
    }

    private fun processUpiPayment() {
        val upiId = etUpiId.text?.toString()?.trim().orEmpty()

        // 1. Validate UPI ID format only
        if (!upiRegex.matches(upiId)) {
            tilUpiId.error = "Please enter a valid UPI ID (e.g. yourname@upi)"
            return
        }
        tilUpiId.error = null

        val user = authRepository.currentUser
        val userId = user?.uid ?: "guest_user"

        // In-button circular progress replacing text
        btnPayUpiNow.text = ""
        btnPayUpiNow.isEnabled = false
        btnProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // 2. Immediately create a Booking document in Firestore with status "pending"
            val pendingResult = bookingRepository.createPendingBooking(
                userId = userId,
                eventId = eventId,
                eventName = eventName,
                numberOfTickets = quantity,
                totalAmount = totalAmount,
                tierId = tierId,
                tierName = tierName
            )

            if (pendingResult.isFailure) {
                btnProgressBar.visibility = View.GONE
                btnPayUpiNow.text = "Pay ${formatCurrency(totalAmount)}"
                btnPayUpiNow.isEnabled = true
                Toast.makeText(
                    this@UpiPaymentActivity,
                    "Failed to create pending booking: ${pendingResult.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val booking = pendingResult.getOrThrow()

            // 3. Simulate processing delay (~1.5–2 seconds)
            delay(1800)

            // Run seat-deduction Firestore transaction
            val confirmResult = bookingRepository.confirmBookingWithSeatDeduction(
                bookingId = booking.bookingId,
                eventId = eventId,
                quantity = quantity,
                tierId = tierId
            )

            btnProgressBar.visibility = View.GONE
            btnPayUpiNow.text = "Pay ${formatCurrency(totalAmount)}"
            btnPayUpiNow.isEnabled = true

            confirmResult.onSuccess {
                // 5. On success, navigate to BookingConfirmationActivity passing bookingId
                val intent = Intent(this@UpiPaymentActivity, BookingConfirmationActivity::class.java).apply {
                    putExtra("bookingId", booking.bookingId)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            }.onFailure { error ->
                // 4. If transaction fails because seats ran out, show error dialog returning to EventDetailActivity
                MaterialAlertDialogBuilder(this@UpiPaymentActivity)
                    .setTitle("Booking Unavailable")
                    .setMessage("Sorry, seats are no longer available for this event.")
                    .setPositiveButton("OK") { _, _ ->
                        val intent = Intent(this@UpiPaymentActivity, EventDetailActivity::class.java).apply {
                            putExtra("eventId", eventId)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }
}
