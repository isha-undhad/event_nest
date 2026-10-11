package com.example.eventnest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventnest.utils.formatCurrency
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class PaymentMethodActivity : BaseActivity() {

    private lateinit var btnBackPayment: View
    private lateinit var tvTopSummaryTotal: TextView
    private lateinit var cardUpi: MaterialCardView
    private lateinit var rbUpi: RadioButton
    private lateinit var cardCard: MaterialCardView
    private lateinit var rbCard: RadioButton
    private lateinit var btnContinue: MaterialButton

    private var eventId: String = ""
    private var eventName: String = ""
    private var tierId: String = ""
    private var tierName: String = ""
    private var quantity: Int = 1
    private var totalAmount: Double = 0.0

    private var selectedMethod: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_method)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.paymentRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        extractExtras()
        initViews()
        populateOrderSummary()
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
        btnBackPayment = findViewById(R.id.btnBackPayment)
        tvTopSummaryTotal = findViewById(R.id.tvTopSummaryTotal)
        cardUpi = findViewById(R.id.cardUpi)
        rbUpi = findViewById(R.id.rbUpi)
        cardCard = findViewById(R.id.cardCard)
        rbCard = findViewById(R.id.rbCard)
        btnContinue = findViewById(R.id.btnContinue)

        btnContinue.isEnabled = false
    }

    private fun populateOrderSummary() {
        // Top summary bar: "Total: ₹<totalAmount>" (bold, formatCurrency())
        tvTopSummaryTotal.text = "Total: ${formatCurrency(totalAmount)}"
    }

    private fun setupListeners() {
        btnBackPayment.setOnClickListener {
            finish()
        }

        cardUpi.setOnClickListener {
            selectMethod("upi")
        }

        cardCard.setOnClickListener {
            selectMethod("card")
        }

        btnContinue.setOnClickListener {
            when (selectedMethod) {
                "upi" -> {
                    val intent = Intent(this, UpiPaymentActivity::class.java).apply {
                        putExtra("eventId", eventId)
                        putExtra("eventName", eventName)
                        putExtra("tierId", tierId)
                        putExtra("tierName", tierName)
                        putExtra("quantity", quantity)
                        putExtra("totalAmount", totalAmount)
                    }
                    startActivity(intent)
                }
                "card" -> {
                    val intent = Intent(this, CardPaymentActivity::class.java).apply {
                        putExtra("eventId", eventId)
                        putExtra("eventName", eventName)
                        putExtra("tierId", tierId)
                        putExtra("tierName", tierName)
                        putExtra("quantity", quantity)
                        putExtra("totalAmount", totalAmount)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun selectMethod(method: String) {
        selectedMethod = method
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val strokeColor = ContextCompat.getColor(this, R.color.card_stroke)
        val selectedStrokeWidth = dpToPx(2)
        val unselectedStrokeWidth = dpToPx(1)

        val isUpi = method == "upi"
        val isCard = method == "card"

        rbUpi.isChecked = isUpi
        cardUpi.strokeColor = if (isUpi) primaryColor else strokeColor
        cardUpi.strokeWidth = if (isUpi) selectedStrokeWidth else unselectedStrokeWidth

        rbCard.isChecked = isCard
        cardCard.strokeColor = if (isCard) primaryColor else strokeColor
        cardCard.strokeWidth = if (isCard) selectedStrokeWidth else unselectedStrokeWidth

        btnContinue.isEnabled = true
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
