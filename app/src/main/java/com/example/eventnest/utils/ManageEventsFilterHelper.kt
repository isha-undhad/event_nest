package com.example.eventnest.utils

import com.example.eventnest.models.Event

object ManageEventsFilterHelper {
    const val FILTER_ALL = "all"
    const val FILTER_ACTIVE = "active"
    const val FILTER_CANCELLED = "cancelled"

    /**
     * Filters a list of events according to the specified status filter.
     */
    fun filterEvents(events: List<Event>, filter: String): List<Event> {
        return when (filter.lowercase()) {
            FILTER_ACTIVE -> events.filter { it.status.equals("active", ignoreCase = true) }
            FILTER_CANCELLED -> events.filter { it.status.equals("cancelled", ignoreCase = true) }
            else -> events
        }
    }
}
