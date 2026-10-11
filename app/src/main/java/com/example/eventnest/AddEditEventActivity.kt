package com.example.eventnest

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.eventnest.models.Category
import com.example.eventnest.models.Event
import com.example.eventnest.repositories.CategoryRepository
import com.example.eventnest.repositories.EventRepository
import com.example.eventnest.utils.EventSeatHelper
import com.example.eventnest.utils.SeatValidationResult
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

import com.example.eventnest.models.TicketTier

class AddEditEventActivity : BaseActivity() {

    private val eventRepository = EventRepository()
    private val categoryRepository = CategoryRepository()

    // Screen mode & state
    private var eventId: String? = null
    private var isEditMode: Boolean = false
    private var existingEvent: Event? = null

    private val categories = mutableListOf<Category>()
    private var selectedCategoryId: String = ""

    // Views
    private lateinit var btnBackAddEdit: ImageView
    private lateinit var tvAddEditTitle: TextView
    private lateinit var tvAddEditSubtitle: TextView

    private lateinit var layoutAddEditLoading: FrameLayout
    private lateinit var scrollAddEditForm: NestedScrollView
    private lateinit var layoutAddEditForm: LinearLayout

    private lateinit var tilEventName: TextInputLayout
    private lateinit var etEventName: TextInputEditText

    private lateinit var tilCategory: TextInputLayout
    private lateinit var actvCategory: AutoCompleteTextView

    private lateinit var tilDescription: TextInputLayout
    private lateinit var etDescription: TextInputEditText

    private lateinit var tilVenue: TextInputLayout
    private lateinit var etVenue: TextInputEditText

    private lateinit var tilDate: TextInputLayout
    private lateinit var etDate: TextInputEditText

    private lateinit var tilTime: TextInputLayout
    private lateinit var etTime: TextInputEditText

    private lateinit var llTiersContainer: LinearLayout
    private lateinit var btnAddTier: MaterialButton

    private lateinit var cardSeatsInfo: View
    private lateinit var layoutEditSeatsBreakdown: LinearLayout
    private lateinit var tvSeatsInfoTitle: TextView
    private lateinit var tvBookedSeatsCount: TextView
    private lateinit var tvCalculatedAvailableSeats: TextView
    private lateinit var tvSeatsNote: TextView

    private lateinit var tilBannerUrl: TextInputLayout
    private lateinit var etBannerUrl: TextInputEditText
    private lateinit var ivBannerPreview: ImageView

    private lateinit var btnSaveEvent: MaterialButton
    private lateinit var progressSaving: ProgressBar

    private data class TierViewHolder(
        var tierId: String,
        val cardView: View,
        val tvBadge: TextView,
        val btnRemoveTier: View,
        val tilTierName: TextInputLayout,
        val etTierName: TextInputEditText,
        val tilPrice: TextInputLayout,
        val etPrice: TextInputEditText,
        val tilTotalSeats: TextInputLayout,
        val etTotalSeats: TextInputEditText,
        val layoutEditBreakdown: View,
        val tvBookedSeats: TextView,
        val tvAvailableSeats: TextView,
        var bookedSeats: Int = 0,
        var calculatedAvailableSeats: Int = 0
    )

    private val tierHolders = mutableListOf<TierViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_event)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addEditRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Determine mode: Edit mode if eventId is provided, otherwise Create mode
        eventId = intent.getStringExtra(EXTRA_EVENT_ID)
            ?: intent.getStringExtra("eventId")
            ?: intent.getStringExtra("EXTRA_EVENT_ID")
        isEditMode = !eventId.isNullOrBlank()

        initViews()
        setupListeners()
        loadCategories()

        if (isEditMode) {
            setupEditMode()
            loadExistingEvent(eventId!!)
        } else {
            setupCreateMode()
        }
    }

    private fun initViews() {
        btnBackAddEdit = findViewById(R.id.btnBackAddEdit)
        tvAddEditTitle = findViewById(R.id.tvAddEditTitle)
        tvAddEditSubtitle = findViewById(R.id.tvAddEditSubtitle)

        layoutAddEditLoading = findViewById(R.id.layoutAddEditLoading)
        scrollAddEditForm = findViewById(R.id.scrollAddEditForm)
        layoutAddEditForm = findViewById(R.id.layoutAddEditForm)

        tilEventName = findViewById(R.id.tilEventName)
        etEventName = findViewById(R.id.etEventName)

        tilCategory = findViewById(R.id.tilCategory)
        actvCategory = findViewById(R.id.actvCategory)

        tilDescription = findViewById(R.id.tilDescription)
        etDescription = findViewById(R.id.etDescription)

        tilVenue = findViewById(R.id.tilVenue)
        etVenue = findViewById(R.id.etVenue)

        tilDate = findViewById(R.id.tilDate)
        etDate = findViewById(R.id.etDate)

        tilTime = findViewById(R.id.tilTime)
        etTime = findViewById(R.id.etTime)

        llTiersContainer = findViewById(R.id.llTiersContainer)
        btnAddTier = findViewById(R.id.btnAddTier)

        cardSeatsInfo = findViewById(R.id.cardSeatsInfo)
        layoutEditSeatsBreakdown = findViewById(R.id.layoutEditSeatsBreakdown)
        tvSeatsInfoTitle = findViewById(R.id.tvSeatsInfoTitle)
        tvBookedSeatsCount = findViewById(R.id.tvBookedSeatsCount)
        tvCalculatedAvailableSeats = findViewById(R.id.tvCalculatedAvailableSeats)
        tvSeatsNote = findViewById(R.id.tvSeatsNote)

        tilBannerUrl = findViewById(R.id.tilBannerUrl)
        etBannerUrl = findViewById(R.id.etBannerUrl)
        ivBannerPreview = findViewById(R.id.ivBannerPreview)

        btnSaveEvent = findViewById(R.id.btnSaveEvent)
        progressSaving = findViewById(R.id.progressSaving)
    }

    private fun createTierViewHolder(tier: TicketTier, isEdit: Boolean, tierIndex: Int): TierViewHolder {
        val tierView = layoutInflater.inflate(R.layout.item_tier_input, llTiersContainer, false)
        val tvBadge: TextView = tierView.findViewById(R.id.tvTierBadge)
        val btnRemoveTier: View = tierView.findViewById(R.id.btnRemoveTier)
        val tilTierName: TextInputLayout = tierView.findViewById(R.id.tilTierName)
        val etTierName: TextInputEditText = tierView.findViewById(R.id.etTierName)
        val tilPrice: TextInputLayout = tierView.findViewById(R.id.tilTierPrice)
        val etPrice: TextInputEditText = tierView.findViewById(R.id.etTierPrice)
        val tilTotalSeats: TextInputLayout = tierView.findViewById(R.id.tilTierTotalSeats)
        val etTotalSeats: TextInputEditText = tierView.findViewById(R.id.etTierTotalSeats)
        val layoutEditBreakdown: View = tierView.findViewById(R.id.layoutTierEditBreakdown)
        val tvBookedSeats: TextView = tierView.findViewById(R.id.tvTierBookedSeats)
        val tvAvailableSeats: TextView = tierView.findViewById(R.id.tvTierAvailableSeats)

        tvBadge.text = "TIER ${tierIndex + 1}"
        etTierName.setText(tier.tierName)

        val priceStr = if (tier.price > 0.0) {
            if (tier.price % 1.0 == 0.0) tier.price.toInt().toString() else tier.price.toString()
        } else ""
        etPrice.setText(priceStr)

        val seatsStr = if (tier.totalSeats > 0) tier.totalSeats.toString() else ""
        etTotalSeats.setText(seatsStr)

        val booked = if (isEdit) EventSeatHelper.calculateBookedSeats(tier.totalSeats, tier.availableSeats) else 0
        val initialAvailable = if (isEdit) tier.availableSeats else tier.totalSeats

        if (isEdit) {
            layoutEditBreakdown.visibility = View.VISIBLE
            tvBookedSeats.text = "Booked: $booked"
            tvAvailableSeats.text = "Available: $initialAvailable"
        } else {
            layoutEditBreakdown.visibility = View.GONE
        }

        val holder = TierViewHolder(
            tierId = tier.tierId,
            cardView = tierView,
            tvBadge = tvBadge,
            btnRemoveTier = btnRemoveTier,
            tilTierName = tilTierName,
            etTierName = etTierName,
            tilPrice = tilPrice,
            etPrice = etPrice,
            tilTotalSeats = tilTotalSeats,
            etTotalSeats = etTotalSeats,
            layoutEditBreakdown = layoutEditBreakdown,
            tvBookedSeats = tvBookedSeats,
            tvAvailableSeats = tvAvailableSeats,
            bookedSeats = booked,
            calculatedAvailableSeats = initialAvailable
        )

        etTierName.doAfterTextChanged {
            tilTierName.error = null
        }

        etPrice.doAfterTextChanged {
            tilPrice.error = null
        }

        etTotalSeats.doAfterTextChanged { text ->
            val input = text?.toString()?.trim().orEmpty()
            val total = input.toIntOrNull()

            if (isEditMode) {
                if (total == null || total <= 0) {
                    tilTotalSeats.error = "Seats must be > 0"
                    tvAvailableSeats.text = "Available: 0"
                    holder.calculatedAvailableSeats = 0
                } else {
                    val validation = EventSeatHelper.validateTotalSeats(total, holder.bookedSeats)
                    when (validation) {
                        is SeatValidationResult.Invalid -> {
                            tilTotalSeats.error = validation.errorMessage
                            tvAvailableSeats.text = "Available: 0"
                            holder.calculatedAvailableSeats = 0
                        }
                        is SeatValidationResult.Valid -> {
                            tilTotalSeats.error = null
                            holder.calculatedAvailableSeats = validation.newAvailableSeats
                            tvAvailableSeats.text = "Available: ${holder.calculatedAvailableSeats}"
                        }
                    }
                }
            } else {
                if (total != null && total > 0) {
                    tilTotalSeats.error = null
                    holder.calculatedAvailableSeats = total
                } else if (input.isNotBlank()) {
                    tilTotalSeats.error = "Seats must be > 0"
                    holder.calculatedAvailableSeats = 0
                } else {
                    holder.calculatedAvailableSeats = 0
                }
            }
            updateAggregateSeatingSummary()
        }

        btnRemoveTier.setOnClickListener {
            if (tierHolders.size <= 1) {
                Toast.makeText(this@AddEditEventActivity, "An event must have at least 1 ticket tier", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isEditMode && holder.bookedSeats > 0) {
                Toast.makeText(this@AddEditEventActivity, "Can't remove a tier that already has bookings", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            llTiersContainer.removeView(holder.cardView)
            tierHolders.remove(holder)
            updateTierBadges()
            updateAddTierButtonState()
            updateAggregateSeatingSummary()
        }

        return holder
    }

    private fun updateTierBadges() {
        tierHolders.forEachIndexed { index, holder ->
            holder.tvBadge.text = "TIER ${index + 1}"
        }
    }

    private fun updateAddTierButtonState() {
        val canAdd = tierHolders.size < 3
        btnAddTier.visibility = if (canAdd) View.VISIBLE else View.GONE
        btnAddTier.isEnabled = canAdd
    }

    private fun getNextSuggestedTier(): TicketTier {
        val existingNames = tierHolders.map { it.etTierName.text?.toString()?.trim().orEmpty().lowercase() }
        return when {
            !existingNames.contains("general") -> TicketTier("tier_general", "General", 499.0, 100, 100)
            !existingNames.contains("vip") -> TicketTier("tier_vip", "VIP", 999.0, 50, 50)
            !existingNames.contains("vvip") -> TicketTier("tier_vvip", "VVIP", 1999.0, 20, 20)
            else -> {
                val num = tierHolders.size + 1
                TicketTier("tier_$num", "Tier $num", 499.0, 50, 50)
            }
        }
    }

    private fun updateAggregateSeatingSummary() {
        val totalBooked = tierHolders.sumOf { it.bookedSeats }
        val totalAvailable = tierHolders.sumOf { it.calculatedAvailableSeats }

        tvBookedSeatsCount.text = totalBooked.toString()
        tvCalculatedAvailableSeats.text = totalAvailable.toString()
    }

    private fun setupCreateMode() {
        tvAddEditTitle.text = "Add Event"
        tvAddEditSubtitle.text = "Enter event details, pricing and seating tiers"
        btnSaveEvent.text = "Publish Event"

        layoutEditSeatsBreakdown.visibility = View.GONE
        tvSeatsInfoTitle.text = "Total Seating Capacity"
        tvSeatsNote.text = "In create mode, available seats equal total seats across all tiers automatically."

        llTiersContainer.removeAllViews()
        tierHolders.clear()

        // Start with just 1 tier row pre-filled as "General" by default
        val defaultTier = TicketTier("tier_general", "General", 499.0, 100, 100)
        val holder = createTierViewHolder(defaultTier, isEdit = false, tierIndex = 0)
        tierHolders.add(holder)
        llTiersContainer.addView(holder.cardView)

        updateAddTierButtonState()
        updateAggregateSeatingSummary()
    }

    private fun setupEditMode() {
        tvAddEditTitle.text = "Edit Event"
        tvAddEditSubtitle.text = "Update event details, pricing and seating tiers"
        btnSaveEvent.text = "Save Changes"

        layoutEditSeatsBreakdown.visibility = View.VISIBLE
        tvSeatsInfoTitle.text = "Total Seat Allocation Breakdown"
        tvSeatsNote.text = "Per-tier seats cannot be reduced below tickets already booked for that tier."
    }

    private fun setupListeners() {
        btnBackAddEdit.setOnClickListener {
            finish()
        }

        // Date Picker
        val openDatePicker = View.OnClickListener {
            showDatePickerDialog()
        }
        etDate.setOnClickListener(openDatePicker)
        tilDate.setEndIconOnClickListener(openDatePicker)

        // Time Picker
        val openTimePicker = View.OnClickListener {
            showTimePickerDialog()
        }
        etTime.setOnClickListener(openTimePicker)
        tilTime.setEndIconOnClickListener(openTimePicker)

        // Live Banner URL preview
        etBannerUrl.doAfterTextChanged { text ->
            val url = text?.toString()?.trim().orEmpty()
            if (url.isNotBlank()) {
                Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_event_placeholder)
                    .error(R.drawable.ic_event_placeholder)
                    .into(ivBannerPreview)
            } else {
                ivBannerPreview.setImageResource(R.drawable.ic_event_placeholder)
            }
        }

        // Clear error states on text change
        etEventName.doAfterTextChanged { tilEventName.error = null }
        etDescription.doAfterTextChanged { tilDescription.error = null }
        etVenue.doAfterTextChanged { tilVenue.error = null }

        // Category selection
        actvCategory.setOnItemClickListener { _, _, position, _ ->
            if (position in categories.indices) {
                selectedCategoryId = categories[position].categoryId
                tilCategory.error = null
            }
        }

        // Save button
        btnSaveEvent.setOnClickListener {
            saveEvent()
        }

        // Add Tier button (1 to 3 tiers max)
        btnAddTier.setOnClickListener {
            if (tierHolders.size >= 3) {
                Toast.makeText(this, "Maximum of 3 tiers allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val suggestedTier = getNextSuggestedTier()
            val holder = createTierViewHolder(suggestedTier, isEdit = false, tierIndex = tierHolders.size)
            tierHolders.add(holder)
            llTiersContainer.addView(holder.cardView)
            updateTierBadges()
            updateAddTierButtonState()
            updateAggregateSeatingSummary()
        }
    }

    private fun showDatePickerDialog() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Event Date")
            .build()

        datePicker.addOnPositiveButtonClickListener { utcTimestamp ->
            // Format UTC timestamp from MaterialDatePicker to readable "dd MMM yyyy"
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = utcTimestamp
            }
            val localCalendar = Calendar.getInstance().apply {
                set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            }
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            etDate.setText(formatter.format(localCalendar.time))
            tilDate.error = null
        }

        datePicker.show(supportFragmentManager, "ADD_EDIT_DATE_PICKER")
    }

    private fun showTimePickerDialog() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(18)
            .setMinute(30)
            .setTitleText("Select Event Time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
            }
            val formatter = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            etTime.setText(formatter.format(cal.time).uppercase())
            tilTime.error = null
        }

        timePicker.show(supportFragmentManager, "ADD_EDIT_TIME_PICKER")
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val result = categoryRepository.getAllCategories()
            categories.clear()

            val fetched = result.getOrNull().orEmpty()
            if (fetched.isNotEmpty()) {
                categories.addAll(fetched)
            } else {
                // Fallback to the 5 default categories
                categories.addAll(
                    listOf(
                        Category("cat_music", "Music", ""),
                        Category("cat_arts", "Performing & Visual Arts", ""),
                        Category("cat_holidays", "Holidays", ""),
                        Category("cat_hobbies", "Hobbies", ""),
                        Category("cat_food", "Food & Drink", "")
                    )
                )
            }

            val categoryNames = categories.map { it.categoryName }
            val adapter = ArrayAdapter(this@AddEditEventActivity, android.R.layout.simple_dropdown_item_1line, categoryNames)
            actvCategory.setAdapter(adapter)

            // If existing event was loaded before categories finished, select it now
            if (selectedCategoryId.isNotBlank()) {
                val match = categories.firstOrNull { it.categoryId == selectedCategoryId }
                if (match != null) {
                    actvCategory.setText(match.categoryName, false)
                }
            }
        }
    }

    private fun loadExistingEvent(id: String) {
        layoutAddEditLoading.visibility = View.VISIBLE
        scrollAddEditForm.visibility = View.GONE

        lifecycleScope.launch {
            val result = eventRepository.getEventById(id)
            val event = result.getOrNull()

            layoutAddEditLoading.visibility = View.GONE
            scrollAddEditForm.visibility = View.VISIBLE

            if (event == null) {
                Toast.makeText(this@AddEditEventActivity, "Event not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            existingEvent = event
            populateForm(event)
        }
    }

    private fun populateForm(event: Event) {
        etEventName.setText(event.eventName)
        etDescription.setText(event.description)
        etVenue.setText(event.venue)
        etDate.setText(event.date)
        etTime.setText(event.time)
        etBannerUrl.setText(event.bannerImageUrl)

        // Prefill tiers
        llTiersContainer.removeAllViews()
        tierHolders.clear()

        val tiersToPopulate = event.effectiveTiers
        for ((index, tier) in tiersToPopulate.withIndex()) {
            val holder = createTierViewHolder(tier, isEdit = true, tierIndex = index)
            tierHolders.add(holder)
            llTiersContainer.addView(holder.cardView)
        }
        updateAddTierButtonState()
        updateAggregateSeatingSummary()

        // Category prefill
        selectedCategoryId = event.categoryId
        if (categories.isNotEmpty()) {
            val match = categories.firstOrNull { it.categoryId == selectedCategoryId }
            if (match != null) {
                actvCategory.setText(match.categoryName, false)
            }
        }
    }

    private fun saveEvent() {
        val name = etEventName.text?.toString()?.trim().orEmpty()
        val desc = etDescription.text?.toString()?.trim().orEmpty()
        val venue = etVenue.text?.toString()?.trim().orEmpty()
        val date = etDate.text?.toString()?.trim().orEmpty()
        val time = etTime.text?.toString()?.trim().orEmpty()
        val bannerUrl = etBannerUrl.text?.toString()?.trim().orEmpty()

        var hasError = false

        if (name.isBlank()) {
            tilEventName.error = "Event name is required"
            hasError = true
        }

        if (selectedCategoryId.isBlank()) {
            tilCategory.error = "Please select a category"
            hasError = true
        }

        if (desc.isBlank()) {
            tilDescription.error = "Description is required"
            hasError = true
        }

        if (venue.isBlank()) {
            tilVenue.error = "Venue is required"
            hasError = true
        }

        if (date.isBlank()) {
            tilDate.error = "Date is required"
            hasError = true
        }

        if (time.isBlank()) {
            tilTime.error = "Time is required"
            hasError = true
        }

        if (tierHolders.isEmpty()) {
            Toast.makeText(this, "At least one ticket tier is required", Toast.LENGTH_SHORT).show()
            return
        }

        // Check tier name uniqueness (case-insensitive)
        val normalizedNames = tierHolders.map { it.etTierName.text?.toString()?.trim().orEmpty().lowercase() }
        val nameCounts = normalizedNames.filter { it.isNotBlank() }.groupingBy { it }.eachCount()

        var tierError = false
        val savedTiers = mutableListOf<TicketTier>()

        for (holder in tierHolders) {
            val tierName = holder.etTierName.text?.toString()?.trim().orEmpty()
            val price = holder.etPrice.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
            val totalSeats = holder.etTotalSeats.text?.toString()?.trim()?.toIntOrNull() ?: 0

            if (tierName.isBlank()) {
                holder.tilTierName.error = "Tier name is required"
                tierError = true
            } else if ((nameCounts[tierName.lowercase()] ?: 0) > 1) {
                holder.tilTierName.error = "Tier names must be unique"
                tierError = true
            } else {
                holder.tilTierName.error = null
            }

            if (price <= 0.0) {
                holder.tilPrice.error = "Price must be > ₹0"
                tierError = true
            } else {
                holder.tilPrice.error = null
            }

            if (totalSeats <= 0) {
                holder.tilTotalSeats.error = "Seats must be > 0"
                tierError = true
            } else if (isEditMode) {
                val validation = EventSeatHelper.validateTotalSeats(totalSeats, holder.bookedSeats)
                if (validation is SeatValidationResult.Invalid) {
                    holder.tilTotalSeats.error = validation.errorMessage
                    tierError = true
                } else {
                    holder.tilTotalSeats.error = null
                }
            } else {
                holder.tilTotalSeats.error = null
            }

            val finalTierAvailableSeats = if (isEditMode) {
                EventSeatHelper.calculateNewAvailableSeats(totalSeats, holder.bookedSeats)
            } else {
                totalSeats
            }

            val baseId = "tier_${tierName.lowercase().replace("[^a-z0-9_]".toRegex(), "_")}"
            val safeId = if (holder.tierId.isNotBlank() && !savedTiers.any { it.tierId == holder.tierId }) {
                holder.tierId
            } else if (!savedTiers.any { it.tierId == baseId }) {
                baseId
            } else {
                "${baseId}_${System.currentTimeMillis()}"
            }

            savedTiers.add(
                TicketTier(
                    tierId = safeId,
                    tierName = tierName,
                    price = price,
                    totalSeats = totalSeats,
                    availableSeats = finalTierAvailableSeats
                )
            )
        }

        if (hasError || tierError) {
            Toast.makeText(this, "Please fix the highlighted errors", Toast.LENGTH_SHORT).show()
            return
        }

        // Aggregate pricing & capacity for full backward compatibility
        val minPrice = savedTiers.minOfOrNull { it.price } ?: 0.0
        val sumTotalSeats = savedTiers.sumOf { it.totalSeats }
        val sumAvailableSeats = savedTiers.sumOf { it.availableSeats }

        // Prepare event document
        val eventToSave = if (isEditMode && existingEvent != null) {
            existingEvent!!.copy(
                eventName = name,
                categoryId = selectedCategoryId,
                description = desc,
                venue = venue,
                date = date,
                time = time,
                tiers = savedTiers,
                price = minPrice,
                totalSeats = sumTotalSeats,
                availableSeats = sumAvailableSeats,
                bannerImageUrl = bannerUrl
            )
        } else {
            Event(
                eventId = "",
                eventName = name,
                categoryId = selectedCategoryId,
                description = desc,
                venue = venue,
                date = date,
                time = time,
                tiers = savedTiers,
                price = minPrice,
                totalSeats = sumTotalSeats,
                availableSeats = sumAvailableSeats,
                bannerImageUrl = bannerUrl,
                status = "active"
            )
        }

        // Show saving UI state
        setSavingState(true)

        lifecycleScope.launch {
            val result = if (isEditMode) {
                eventRepository.updateEvent(eventToSave)
            } else {
                eventRepository.createEvent(eventToSave).map { }
            }

            setSavingState(false)

            if (result.isSuccess) {
                val message = if (isEditMode) "Event updated successfully!" else "Event published successfully!"
                Toast.makeText(this@AddEditEventActivity, message, Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to save event"
                Toast.makeText(this@AddEditEventActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setSavingState(isSaving: Boolean) {
        btnSaveEvent.isEnabled = !isSaving
        progressSaving.visibility = if (isSaving) View.VISIBLE else View.GONE
        if (isSaving) {
            btnSaveEvent.text = ""
        } else {
            btnSaveEvent.text = if (isEditMode) "Save Changes" else "Publish Event"
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
    }
}
