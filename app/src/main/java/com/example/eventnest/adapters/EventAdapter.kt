package com.example.eventnest.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eventnest.R
import com.example.eventnest.models.Event
import com.example.eventnest.utils.formatCurrency

class EventAdapter(
    private var events: List<Event> = emptyList(),
    private var categoryNameMap: Map<String, String> = emptyMap(),
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    fun submitList(newEvents: List<Event>, newCategoryMap: Map<String, String> = categoryNameMap) {
        events = newEvents
        categoryNameMap = newCategoryMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_card, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivEventBanner: ImageView = itemView.findViewById(R.id.ivEventBanner)
        private val tvCategoryTag: TextView = itemView.findViewById(R.id.tvCategoryTag)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        private val tvEventTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        private val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        private val tvEventTime: TextView = itemView.findViewById(R.id.tvEventTime)
        private val tvEventVenue: TextView = itemView.findViewById(R.id.tvEventVenue)
        private val tvEventPrice: TextView = itemView.findViewById(R.id.tvEventPrice)
        private val tvAvailableSeats: TextView = itemView.findViewById(R.id.tvAvailableSeats)

        fun bind(event: Event) {
            tvEventTitle.text = event.eventName
            tvEventDate.text = event.date.ifBlank { "Date TBA" }
            tvEventTime.text = event.time.ifBlank { "Time TBA" }
            tvEventVenue.text = event.venue.ifBlank { "Venue TBA" }

            // Category tag
            val categoryName = categoryNameMap[event.categoryId] ?: "EVENT"
            tvCategoryTag.text = categoryName.uppercase()

            val availSeats = event.totalAvailableSeats

            // Status badge
            val statusText = if (availSeats <= 0) {
                itemView.context.getString(R.string.sold_out)
            } else {
                event.status.ifBlank { "UPCOMING" }
            }
            tvStatusBadge.text = statusText.uppercase()

            // Price formatted strictly with ₹ symbol via formatCurrency helper
            val minP = event.minPrice
            val maxP = event.maxPrice
            tvEventPrice.text = when {
                minP <= 0.0 -> itemView.context.getString(R.string.price_free)
                minP == maxP -> formatCurrency(minP)
                else -> "From ${formatCurrency(minP)}"
            }

            // Seats left
            if (availSeats <= 0) {
                tvAvailableSeats.text = itemView.context.getString(R.string.sold_out)
                tvAvailableSeats.setTextColor(ContextCompat.getColor(itemView.context, R.color.error))
            } else {
                tvAvailableSeats.text = itemView.context.getString(R.string.seats_left_format, availSeats)
                tvAvailableSeats.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            }

            // Banner Image loaded with Glide
            if (event.bannerImageUrl.isNotBlank()) {
                Glide.with(itemView.context)
                    .load(event.bannerImageUrl)
                    .placeholder(R.drawable.ic_event_placeholder)
                    .error(R.drawable.ic_event_placeholder)
                    .centerCrop()
                    .into(ivEventBanner)
            } else {
                ivEventBanner.setImageResource(R.drawable.ic_event_placeholder)
            }

            itemView.setOnClickListener {
                onEventClick(event)
            }
        }
    }
}
