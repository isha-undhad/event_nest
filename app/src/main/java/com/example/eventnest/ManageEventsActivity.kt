package com.example.eventnest

import android.content.Intent
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
import com.example.eventnest.adapters.ManageEventAdapter
import com.example.eventnest.models.Event
import com.example.eventnest.repositories.EventRepository
import com.example.eventnest.utils.ManageEventsFilterHelper
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ManageEventsActivity : BaseActivity() {

    private val eventRepository = EventRepository()

    // Views
    private lateinit var btnBackManageEvents: View
    private lateinit var btnAddEventManage: View
    private lateinit var chipGroupManageFilter: ChipGroup
    private lateinit var progressManageEvents: ProgressBar
    private lateinit var rvManageEvents: RecyclerView
    private lateinit var layoutManageEventsEmpty: View
    private lateinit var btnEmptyAddEvent: View

    private lateinit var adapter: ManageEventAdapter

    // State & real-time listener
    private var allEvents: List<Event> = emptyList()
    private var currentFilter: String = ManageEventsFilterHelper.FILTER_ALL
    private var eventsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_events)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manageEventsRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupRecyclerView()
        setupListeners()
    }

    private fun initViews() {
        btnBackManageEvents = findViewById(R.id.btnBackManageEvents)
        btnAddEventManage = findViewById(R.id.btnAddEventManage)
        chipGroupManageFilter = findViewById(R.id.chipGroupManageFilter)
        progressManageEvents = findViewById(R.id.progressManageEvents)
        rvManageEvents = findViewById(R.id.rvManageEvents)
        layoutManageEventsEmpty = findViewById(R.id.layoutManageEventsEmpty)
        btnEmptyAddEvent = findViewById(R.id.btnEmptyAddEvent)
    }

    private fun setupRecyclerView() {
        adapter = ManageEventAdapter(
            onEditClick = { event ->
                navigateToEditEvent(event)
            },
            onDeleteClick = { event ->
                showCancelEventConfirmation(event)
            },
            onItemClick = { event ->
                navigateToEditEvent(event)
            }
        )

        rvManageEvents.layoutManager = LinearLayoutManager(this)
        rvManageEvents.adapter = adapter
    }

    private fun setupListeners() {
        btnBackManageEvents.setOnClickListener {
            finish()
        }

        btnAddEventManage.setOnClickListener {
            navigateToAddEvent()
        }

        btnEmptyAddEvent.setOnClickListener {
            navigateToAddEvent()
        }

        chipGroupManageFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipFilterAll
            currentFilter = when (checkedId) {
                R.id.chipFilterActive -> ManageEventsFilterHelper.FILTER_ACTIVE
                R.id.chipFilterCancelled -> ManageEventsFilterHelper.FILTER_CANCELLED
                else -> ManageEventsFilterHelper.FILTER_ALL
            }
            applyFilter()
        }
    }

    override fun onStart() {
        super.onStart()
        startEventsListener()
    }

    override fun onStop() {
        super.onStop()
        stopEventsListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopEventsListener()
    }

    /**
     * Attaches a live snapshot listener on the events collection to receive dynamic updates.
     */
    private fun startEventsListener() {
        stopEventsListener()

        if (allEvents.isEmpty()) {
            progressManageEvents.visibility = View.VISIBLE
        }

        eventsListener = eventRepository.listenToAllEvents(
            onUpdate = { events ->
                progressManageEvents.visibility = View.GONE
                allEvents = events
                applyFilter()
            },
            onError = { error ->
                progressManageEvents.visibility = View.GONE
                Toast.makeText(this@ManageEventsActivity, "Error loading events: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * Removes the snapshot listener to prevent leaks.
     */
    private fun stopEventsListener() {
        eventsListener?.remove()
        eventsListener = null
    }

    private fun applyFilter() {
        val filtered = ManageEventsFilterHelper.filterEvents(allEvents, currentFilter)
        adapter.submitList(filtered)

        if (filtered.isEmpty()) {
            rvManageEvents.visibility = View.GONE
            layoutManageEventsEmpty.visibility = View.VISIBLE
        } else {
            rvManageEvents.visibility = View.VISIBLE
            layoutManageEventsEmpty.visibility = View.GONE
        }
    }

    private fun navigateToAddEvent() {
        val intent = Intent(this, AddEditEventActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToEditEvent(event: Event) {
        val intent = Intent(this, AddEditEventActivity::class.java).apply {
            putExtra(AddEditEventActivity.EXTRA_EVENT_ID, event.eventId)
        }
        startActivity(intent)
    }

    /**
     * Displays a confirmation dialog before performing a soft-delete on the event.
     */
    private fun showCancelEventConfirmation(event: Event) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cancel Event")
            .setMessage("Cancel this event? Existing bookings will be kept.")
            .setPositiveButton("Cancel Event") { _, _ ->
                cancelEvent(event)
            }
            .setNegativeButton("Keep Event", null)
            .show()
    }

    private fun cancelEvent(event: Event) {
        lifecycleScope.launch {
            val result = eventRepository.softDeleteEvent(event.eventId)
            if (result.isSuccess) {
                Toast.makeText(
                    this@ManageEventsActivity,
                    "Event \"${event.eventName}\" has been cancelled.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to cancel event"
                Toast.makeText(this@ManageEventsActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
