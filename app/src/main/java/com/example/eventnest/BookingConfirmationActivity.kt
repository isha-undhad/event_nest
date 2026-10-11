package com.example.eventnest

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.eventnest.models.Booking
import com.example.eventnest.models.Event
import com.example.eventnest.repositories.BookingRepository
import com.example.eventnest.repositories.EventRepository
import com.example.eventnest.utils.formatCurrency
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BookingConfirmationActivity : BaseActivity() {

    private val bookingRepository = BookingRepository()
    private val eventRepository = EventRepository()

    private lateinit var llReadOnlyHeader: View
    private lateinit var btnBackConfirm: View
    private lateinit var tvConfirmHeading: TextView
    private lateinit var tvConfirmSubheading: TextView
    private lateinit var tvConfirmStatusBadge: TextView
    private lateinit var tvConfirmEventName: TextView
    private lateinit var tvConfirmDate: TextView
    private lateinit var tvConfirmTime: TextView
    private lateinit var tvConfirmVenue: TextView
    private lateinit var tvConfirmTickets: TextView
    private lateinit var tvConfirmBookingId: TextView
    private lateinit var tvConfirmTotalPaid: TextView
    private lateinit var ivQrCode: ImageView
    private lateinit var btnDownloadPdf: MaterialButton
    private lateinit var btnDone: MaterialButton
    private lateinit var btnCancelBooking: MaterialButton

    private var bookingId: String = ""
    private var isReadOnly: Boolean = false
    private var loadedBooking: Booking? = null
    private var loadedEvent: Event? = null
    private var qrCodeBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_confirmation)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.confirmRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bookingId = intent.getStringExtra("bookingId").orEmpty()
        isReadOnly = intent.getBooleanExtra("isReadOnly", false)

        initViews()
        setupListeners()
        loadBookingAndEventDetails()
    }

    private fun initViews() {
        llReadOnlyHeader = findViewById(R.id.llReadOnlyHeader)
        btnBackConfirm = findViewById(R.id.btnBackConfirm)
        tvConfirmHeading = findViewById(R.id.tvConfirmHeading)
        tvConfirmSubheading = findViewById(R.id.tvConfirmSubheading)
        tvConfirmStatusBadge = findViewById(R.id.tvConfirmStatusBadge)
        tvConfirmEventName = findViewById(R.id.tvConfirmEventName)
        tvConfirmDate = findViewById(R.id.tvConfirmDate)
        tvConfirmTime = findViewById(R.id.tvConfirmTime)
        tvConfirmVenue = findViewById(R.id.tvConfirmVenue)
        tvConfirmTickets = findViewById(R.id.tvConfirmTickets)
        tvConfirmBookingId = findViewById(R.id.tvConfirmBookingId)
        tvConfirmTotalPaid = findViewById(R.id.tvConfirmTotalPaid)
        ivQrCode = findViewById(R.id.ivQrCode)
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf)
        btnDone = findViewById(R.id.btnDone)
        btnCancelBooking = findViewById(R.id.btnCancelBooking)

        tvConfirmBookingId.text = if (bookingId.isNotBlank()) "ID: #$bookingId" else "—"

        if (isReadOnly) {
            llReadOnlyHeader.visibility = View.VISIBLE
            btnDownloadPdf.visibility = View.GONE
            btnDone.visibility = View.GONE
        } else {
            llReadOnlyHeader.visibility = View.GONE
            btnDownloadPdf.visibility = View.VISIBLE
            btnDone.visibility = View.VISIBLE
            btnCancelBooking.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        btnBackConfirm.setOnClickListener {
            finish()
        }

        btnDone.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        btnDownloadPdf.setOnClickListener {
            downloadTicketPdf()
        }

        btnCancelBooking.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun showCancelConfirmationDialog() {
        val booking = loadedBooking ?: return

        MaterialAlertDialogBuilder(this)
            .setTitle("Cancel this booking?")
            .setMessage("Are you sure you want to cancel your booking for ${booking.eventName}? This action cannot be undone.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                processCancellation(booking)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun processCancellation(booking: Booking) {
        btnCancelBooking.isEnabled = false

        lifecycleScope.launch {
            val result = bookingRepository.cancelBooking(
                bookingId = booking.bookingId,
                eventId = booking.eventId,
                quantity = booking.numberOfTickets
            )

            result.onSuccess {
                booking.status = "cancelled"
                tvConfirmStatusBadge.text = "CANCELLED"
                tvConfirmStatusBadge.setBackgroundResource(R.drawable.bg_badge_muted)
                tvConfirmStatusBadge.setTextColor(ContextCompat.getColor(this@BookingConfirmationActivity, R.color.text_secondary))
                tvConfirmHeading.text = "Booking Cancelled"
                tvConfirmSubheading.text = "This booking has been cancelled and seats have been released."
                btnCancelBooking.visibility = View.GONE
                setResult(RESULT_OK)
                Toast.makeText(this@BookingConfirmationActivity, "Booking cancelled", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                btnCancelBooking.isEnabled = true
                Toast.makeText(
                    this@BookingConfirmationActivity,
                    "Failed to cancel: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadBookingAndEventDetails() {
        if (bookingId.isBlank()) return

        generateAndDisplayQrCode(bookingId)

        lifecycleScope.launch {
            val bookingResult = bookingRepository.getBookingById(bookingId)
            bookingResult.onSuccess { booking ->
                if (booking != null) {
                    loadedBooking = booking
                    bindBookingData(booking)

                    val eventResult = eventRepository.getEventById(booking.eventId)
                    eventResult.onSuccess { event ->
                        if (event != null) {
                            loadedEvent = event
                            bindEventData(event)
                        }
                    }
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to load booking: ${e.message}", e)
            }
        }
    }

    private fun generateAndDisplayQrCode(content: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 400, 400)
                qrCodeBitmap = bitmap
                withContext(Dispatchers.Main) {
                    ivQrCode.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate QR code: ${e.message}", e)
            }
        }
    }

    private fun bindBookingData(booking: Booking) {
        tvConfirmEventName.text = booking.eventName
        tvConfirmBookingId.text = "ID: #${booking.bookingId}"
        val ticketBase = if (booking.numberOfTickets == 1) "1 Ticket" else "${booking.numberOfTickets} Tickets"
        tvConfirmTickets.text = if (booking.tierName.isNotBlank()) "$ticketBase • ${booking.tierName}" else ticketBase
        tvConfirmTotalPaid.text = "Total Paid = ${formatCurrency(booking.totalAmount)}"
        tvConfirmDate.text = booking.bookingDate
        tvConfirmTime.text = ""

        val isConfirmed = booking.status.equals("confirmed", ignoreCase = true)
        val isPending = booking.status.equals("pending", ignoreCase = true)
        val isCancelled = booking.status.equals("cancelled", ignoreCase = true)

        when {
            isConfirmed -> {
                tvConfirmStatusBadge.text = "CONFIRMED"
                tvConfirmStatusBadge.setBackgroundResource(R.drawable.bg_badge_success)
                tvConfirmStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.success))
                if (isReadOnly) {
                    btnCancelBooking.visibility = View.VISIBLE
                    btnCancelBooking.isEnabled = true
                }
            }
            isPending -> {
                tvConfirmStatusBadge.text = "PENDING"
                tvConfirmStatusBadge.setBackgroundResource(R.drawable.bg_badge_warning)
                tvConfirmStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.warning))
                btnCancelBooking.visibility = View.GONE
            }
            isCancelled -> {
                tvConfirmStatusBadge.text = "CANCELLED"
                tvConfirmStatusBadge.setBackgroundResource(R.drawable.bg_badge_muted)
                tvConfirmStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                tvConfirmHeading.text = "Booking Cancelled"
                tvConfirmSubheading.text = "This booking has been cancelled and seats have been released."
                btnCancelBooking.visibility = View.GONE
            }
        }
    }

    private fun bindEventData(event: Event) {
        if (event.date.isNotBlank()) tvConfirmDate.text = event.date
        if (event.time.isNotBlank()) tvConfirmTime.text = event.time
        if (event.venue.isNotBlank()) tvConfirmVenue.text = event.venue
    }

    private fun downloadTicketPdf() {
        val booking = loadedBooking ?: run {
            Toast.makeText(this, "Ticket details still loading...", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // 1. Draw Brand Header
            paint.color = Color.parseColor("#4F46E5")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, paint)

            paint.color = Color.WHITE
            paint.textSize = 28f
            paint.isFakeBoldText = true
            canvas.drawText("EventNest", 40f, 60f, paint)

            paint.textSize = 13f
            paint.isFakeBoldText = false
            canvas.drawText("Official Booking Confirmation & E-Ticket", 40f, 85f, paint)

            // 2. Success Status Banner
            paint.color = Color.parseColor("#D1FAE5")
            val statusRect = RectF(40f, 130f, (pageWidth - 40).toFloat(), 180f)
            canvas.drawRoundRect(statusRect, 8f, 8f, paint)

            paint.color = Color.parseColor("#10B981")
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("✓  Booking Confirmed!", 60f, 162f, paint)

            // 3. Ticket Card Container
            paint.color = Color.parseColor("#F8FAFC")
            val ticketCard = RectF(40f, 200f, (pageWidth - 40).toFloat(), 500f)
            canvas.drawRoundRect(ticketCard, 12f, 12f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawRoundRect(ticketCard, 12f, 12f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 22f
            paint.isFakeBoldText = true
            canvas.drawText(booking.eventName, 60f, 240f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#64748B")

            val eventDate = loadedEvent?.date ?: booking.bookingDate
            val eventTime = loadedEvent?.time ?: ""
            val eventVenue = loadedEvent?.venue ?: "Venue TBA"

            canvas.drawText("Date: $eventDate", 60f, 275f, paint)
            if (eventTime.isNotBlank()) {
                canvas.drawText("Time: $eventTime", 280f, 275f, paint)
            }
            canvas.drawText("Venue: $eventVenue", 60f, 305f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.isFakeBoldText = true
            canvas.drawText("Tickets: ${booking.numberOfTickets} Ticket(s)", 60f, 335f, paint)

            // 4. Dashed Tear Line
            val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 2f
                pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f)
            }
            canvas.drawLine(40f, 370f, (pageWidth - 40).toFloat(), 370f, dashPaint)

            // 5. Lower Ticket Section
            paint.textSize = 13f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#64748B")
            canvas.drawText("Booking Reference: #${booking.bookingId}", 60f, 410f, paint)

            paint.textSize = 18f
            paint.isFakeBoldText = true
            paint.color = Color.parseColor("#4F46E5")
            canvas.drawText("Total Paid = ${formatCurrency(booking.totalAmount)}", 60f, 445f, paint)

            // 6. QR Code
            val qr = qrCodeBitmap
            if (qr != null) {
                val qrSize = 180
                val qrLeft = (pageWidth - qrSize) / 2f
                val qrTop = 530f
                val scaled = Bitmap.createScaledBitmap(qr, qrSize, qrSize, false)
                canvas.drawBitmap(scaled, qrLeft, qrTop, null)

                paint.textSize = 12f
                paint.isFakeBoldText = false
                paint.color = Color.parseColor("#94A3B8")
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("Show this QR code at the event entrance", (pageWidth / 2).toFloat(), qrTop + qrSize + 25f, paint)
                paint.textAlign = Paint.Align.LEFT
            }

            // 7. Footer
            paint.textSize = 11f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#94A3B8")
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Generated by EventNest • Please bring a valid ID along with this ticket.", (pageWidth / 2).toFloat(), 790f, paint)

            pdfDocument.finishPage(page)

            val fileName = "EventNest_Ticket_${booking.bookingId}.pdf"
            var isSaved = false

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EventNest")
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            pdfDocument.writeTo(out)
                            isSaved = true
                        }
                    }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EventNest")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { out ->
                        pdfDocument.writeTo(out)
                        isSaved = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing PDF: ${e.message}", e)
            } finally {
                pdfDocument.close()
            }

            withContext(Dispatchers.Main) {
                if (isSaved) {
                    Snackbar.make(findViewById(R.id.confirmRoot), "Ticket saved to Downloads", Snackbar.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@BookingConfirmationActivity, "Failed to save ticket PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BookingConfirmation"
    }
}
