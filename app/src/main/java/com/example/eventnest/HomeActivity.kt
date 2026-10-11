package com.example.eventnest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventnest.adapters.HomeEventAdapter
import com.example.eventnest.models.Category
import com.example.eventnest.models.Event
import com.example.eventnest.repositories.CategoryRepository
import com.example.eventnest.repositories.EventRepository
import com.example.eventnest.utils.NavigationUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class HomeActivity : BaseActivity() {

    private val authRepository = AuthRepository()
    private val categoryRepository = CategoryRepository()
    private val eventRepository = EventRepository()

    private lateinit var btnTopProfile: View
    private lateinit var tabLayoutCategories: TabLayout
    private lateinit var rvCategoryEvents: RecyclerView
    private lateinit var llEmptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var eventAdapter: HomeEventAdapter

    // Exactly 5 target categories required by the specification
    private val defaultCategories = listOf(
        Category(categoryId = "cat_music", categoryName = "Music"),
        Category(categoryId = "cat_arts", categoryName = "Performing & Visual Arts"),
        Category(categoryId = "cat_holidays", categoryName = "Holidays"),
        Category(categoryId = "cat_hobbies", categoryName = "Hobbies"),
        Category(categoryId = "cat_food", categoryName = "Food & Drink")
    )

    private var activeCategories: List<Category> = defaultCategories
    private var selectedCategoryIndex = 0
    private var isAdminPreview: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Session gatekeeper
        if (!authRepository.isUserLoggedIn) {
            navigateToLogin()
            return
        }

        isAdminPreview = intent.getBooleanExtra("isAdminPreview", false)
        setContentView(R.layout.activity_home)

        initViews()
        NavigationUtils.setupBottomNavInsets(findViewById(R.id.homeRoot), bottomNav)
        setupRecyclerView()
        setupListeners()
        loadCategoriesAndInitialEvents()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        isAdminPreview = intent.getBooleanExtra("isAdminPreview", false)
        findViewById<View?>(R.id.incAdminPreviewBanner)?.visibility = if (isAdminPreview) View.VISIBLE else View.GONE
        setupExitAdminPreview()
    }

    private fun initViews() {
        btnTopProfile = findViewById(R.id.btnTopProfile)
        tabLayoutCategories = findViewById(R.id.tabLayoutCategories)
        rvCategoryEvents = findViewById(R.id.rvCategoryEvents)
        llEmptyState = findViewById(R.id.llEmptyState)
        progressBar = findViewById(R.id.progressBar)
        bottomNav = findViewById(R.id.bottomNav)

        findViewById<View>(R.id.incAdminPreviewBanner).visibility = if (isAdminPreview) View.VISIBLE else View.GONE
        setupExitAdminPreview()

        // Highlight Home in bottom nav
        bottomNav.selectedItemId = R.id.action_home
    }

    private fun setupExitAdminPreview() {
        val banner = findViewById<View?>(R.id.incAdminPreviewBanner)
        banner?.findViewById<View>(R.id.btnExitAdminPreview)?.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.action_home
    }

    private fun setupRecyclerView() {
        eventAdapter = HomeEventAdapter(
            onEventClick = { eventId ->
                openEventDetail(eventId)
            }
        )
        rvCategoryEvents.layoutManager = LinearLayoutManager(this)
        rvCategoryEvents.adapter = eventAdapter
    }

    private fun setupListeners() {
        // Top right profile button
        btnTopProfile.setOnClickListener {
            openProfile()
        }

        // TabLayout category selection
        tabLayoutCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                if (position in activeCategories.indices) {
                    selectedCategoryIndex = position
                    val selectedCat = activeCategories[position]
                    loadEventsForCategory(selectedCat.categoryId)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Reload on re-tap
                val position = tab?.position ?: 0
                if (position in activeCategories.indices) {
                    val selectedCat = activeCategories[position]
                    loadEventsForCategory(selectedCat.categoryId)
                }
            }
        })

        // Bottom Navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    // Already on Home
                    true
                }
                R.id.action_my_tickets -> {
                    openMyTickets()
                    true
                }
                R.id.action_profile -> {
                    openProfile()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadCategoriesAndInitialEvents() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE

            // Fetch categories collection once on screen load
            val catResult = categoryRepository.getAllCategories()
            catResult.onSuccess { remoteCategories ->
                if (remoteCategories.isNotEmpty()) {
                    // Match or map to the 5 categories
                    val mappedList = mutableListOf<Category>()
                    for (defaultCat in defaultCategories) {
                        val matched = remoteCategories.find {
                            it.categoryName.equals(defaultCat.categoryName, ignoreCase = true) ||
                            it.categoryId.equals(defaultCat.categoryId, ignoreCase = true)
                        }
                        mappedList.add(matched ?: defaultCat)
                    }
                    activeCategories = mappedList
                } else {
                    // Fall back to hardcoded list of the 5 names so UI never breaks
                    activeCategories = defaultCategories
                    categoryRepository.seedDefaultCategoriesIfEmpty()
                }
            }.onFailure { e ->
                Log.w(TAG, "Categories fetch failed, using fallback: ${e.message}")
                activeCategories = defaultCategories
            }

            // Ensure sample events exist if database was empty
            eventRepository.seedSampleEventsIfEmpty()

            setupTabs(activeCategories)

            // Load events for the first category
            if (activeCategories.isNotEmpty()) {
                loadEventsForCategory(activeCategories[0].categoryId)
            } else {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupTabs(categories: List<Category>) {
        tabLayoutCategories.removeAllTabs()
        for (category in categories) {
            val tab = tabLayoutCategories.newTab().setText(category.categoryName)
            tabLayoutCategories.addTab(tab)
        }
    }

    private fun loadEventsForCategory(categoryId: String) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            llEmptyState.visibility = View.GONE
            rvCategoryEvents.visibility = View.GONE

            // Query events where categoryId == given id AND status == "active" ordered by date ascending
            val events = eventRepository.getEventsByCategory(categoryId)
            progressBar.visibility = View.GONE

            if (events.isEmpty()) {
                llEmptyState.visibility = View.VISIBLE
                rvCategoryEvents.visibility = View.GONE
            } else {
                llEmptyState.visibility = View.GONE
                rvCategoryEvents.visibility = View.VISIBLE
                eventAdapter.submitList(events)
            }
        }
    }

    private fun openEventDetail(eventId: String) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            putExtra("eventId", eventId)
            putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
            putExtra("isAdminPreview", isAdminPreview)
        }
        startActivity(intent)
    }

    private fun openProfile() {
        startActivity(Intent(this, ProfileActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("isAdminPreview", isAdminPreview)
        })
    }

    private fun openMyTickets() {
        startActivity(Intent(this, MyTicketsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("isAdminPreview", isAdminPreview)
        })
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "HomeActivity"
    }
}
