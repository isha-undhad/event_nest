package com.example.eventnest

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import java.util.Calendar

class CardPaymentActivity : BaseActivity() {

    private val authRepository = AuthRepository()
    private val bookingRepository = BookingRepository()

    private lateinit var btnBackCard: View
    private lateinit var tvCardTotalAmount: TextView
    private lateinit var tvCardEventSubtitle: TextView
    private lateinit var tilCardNumber: TextInputLayout
    private lateinit var etCardNumber: TextInputEditText
    private lateinit var tilExpiry: TextInputLayout
    private lateinit var etExpiry: TextInputEditText
    private lateinit var tilCvv: TextInputLayout
    private lateinit var etCvv: TextInputEditText
    private lateinit var tilCardName: TextInputLayout
    private lateinit var etCardName: TextInputEditText
    private lateinit var btnPayCardNow: MaterialButton
    private lateinit var btnProgressBar: CircularProgressIndicator

    private var eventId: String = ""
    private var eventName: String = ""
    private var tierId: String = ""
    private var tierName: String = ""
    private var quantity: Int = 1
    private var totalAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_payment)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardPaymentRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        extractExtras()
        initViews()
        populateDetails()
        setupFormatters()
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
        btnBackCard = findViewById(R.id.btnBackCard)
        tvCardTotalAmount = findViewById(R.id.tvCardTotalAmount)
        tvCardEventSubtitle = findViewById(R.id.tvCardEventSubtitle)
        tilCardNumber = findViewById(R.id.tilCardNumber)
        etCardNumber = findViewById(R.id.etCardNumber)
        tilExpiry = findViewById(R.id.tilExpiry)
        etExpiry = findViewById(R.id.etExpiry)
        tilCvv = findViewById(R.id.tilCvv)
        etCvv = findViewById(R.id.etCvv)
        tilCardName = findViewById(R.id.tilCardName)
        etCardName = findViewById(R.id.etCardName)
        btnPayCardNow = findViewById(R.id.btnPayCardNow)
        btnProgressBar = findViewById(R.id.btnProgressBar)
    }

    private fun populateDetails() {
        val formattedTotal = formatCurrency(totalAmount)
        tvCardTotalAmount.text = formattedTotal
        val ticketText = if (quantity == 1) "1 Ticket" else "$quantity Tickets"
        val tierSnippet = if (tierName.isNotBlank()) " • $tierName" else ""
        tvCardEventSubtitle.text = "$eventName$tierSnippet • $ticketText"
        btnPayCardNow.text = "Pay $formattedTotal"
    }

    private fun setupFormatters() {
        // Formats/groups digits into blocks of 4 (max 16 digits total)
        etCardNumber.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                val digits = s.toString().replace(" ", "").take(16)
                val formatted = StringBuilder()
                for (i in digits.indices) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ")
                    }
                    formatted.append(digits[i])
                }

                s.replace(0, s.length, formatted.toString())
                isFormatting = false
            }
        })

        // Formats expiry as MM/YY, auto-inserting slash after month
        etExpiry.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                val digits = s.toString().replace("/", "").replace(" ", "").take(4)
                val formatted = StringBuilder()
                for (i in digits.indices) {
                    if (i == 2) {
                        formatted.append("/")
                    }
                    formatted.append(digits[i])
                }

                s.replace(0, s.length, formatted.toString())
                isFormatting = false
            }
        })
    }

    private fun setupListeners() {
        btnBackCard.setOnClickListener {
            finish()
        }

        btnPayCardNow.setOnClickListener {
            processCardPayment()
        }
    }

    private fun processCardPayment() {
        var hasError = false

        // 1. Card number validation: must be 16 digits
        val rawCardNumber = etCardNumber.text?.toString()?.replace(" ", "").orEmpty()
        if (rawCardNumber.length != 16 || !rawCardNumber.all { it.isDigit() }) {
            tilCardNumber.error = "Enter a valid 16-digit card number"
            hasError = true
        } else {
            tilCardNumber.error = null
        }

        // 2. Expiry validation: valid not-yet-passed MM/YY
        val expiryStr = etExpiry.text?.toString()?.trim().orEmpty()
        if (!isValidExpiry(expiryStr)) {
            tilExpiry.error = "Enter a valid future MM/YY"
            hasError = true
        } else {
            tilExpiry.error = null
        }

        // 3. CVV validation: exactly 3 digits
        val cvvStr = etCvv.text?.toString()?.trim().orEmpty()
        if (cvvStr.length != 3 || !cvvStr.all { it.isDigit() }) {
            tilCvv.error = "CVV must be 3 digits"
            hasError = true
        } else {
            tilCvv.error = null
        }

        // 4. Cardholder name: non-empty
        val nameStr = etCardName.text?.toString()?.trim().orEmpty()
        if (nameStr.isBlank()) {
            tilCardName.error = "Enter cardholder name"
            hasError = true
        } else {
            tilCardName.error = null
        }

        if (hasError) return

        val user = authRepository.currentUser
        val userId = user?.uid ?: "guest_user"

        // In-button circular progress replacing text
        btnPayCardNow.text = ""
        btnPayCardNow.isEnabled = false
        btnProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Immediately create a Booking document in Firestore with status "pending"
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
                btnPayCardNow.text = "Pay ${formatCurrency(totalAmount)}"
                btnPayCardNow.isEnabled = true
                Toast.makeText(
                    this@CardPaymentActivity,
                    "Failed to create pending booking: ${pendingResult.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val booking = pendingResult.getOrThrow()

            // Simulate processing delay (~1.5–2 seconds)
            delay(1800)

            // Run seat-deduction Firestore transaction
            val confirmResult = bookingRepository.confirmBookingWithSeatDeduction(
                bookingId = booking.bookingId,
                eventId = eventId,
                quantity = quantity,
                tierId = tierId
            )

            btnProgressBar.visibility = View.GONE
            btnPayCardNow.text = "Pay ${formatCurrency(totalAmount)}"
            btnPayCardNow.isEnabled = true

            confirmResult.onSuccess {
                // On success, navigate to BookingConfirmationActivity passing bookingId
                val intent = Intent(this@CardPaymentActivity, BookingConfirmationActivity::class.java).apply {
                    putExtra("bookingId", booking.bookingId)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            }.onFailure { error ->
                // On failure (seats ran out), show error dialog returning to EventDetailActivity
                MaterialAlertDialogBuilder(this@CardPaymentActivity)
                    .setTitle("Booking Unavailable")
                    .setMessage("Sorry, seats are no longer available for this event.")
                    .setPositiveButton("OK") { _, _ ->
                        val intent = Intent(this@CardPaymentActivity, EventDetailActivity::class.java).apply {
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

    companion object {
        fun isValidExpiry(expiry: String, calendar: Calendar = Calendar.getInstance()): Boolean {
            val parts = expiry.split("/")
            if (parts.size != 2) return false
            val month = parts[0].toIntOrNull() ?: return false
            val yearSuffix = parts[1].toIntOrNull() ?: return false

            if (month !in 1..12) return false

            val currentYearTwoDigits = calendar.get(Calendar.YEAR) % 100
            val currentMonth = calendar.get(Calendar.MONTH) + 1

            return when {
                yearSuffix > currentYearTwoDigits -> true
                yearSuffix == currentYearTwoDigits -> month >= currentMonth
                else -> false
            }
        }
    }
}
