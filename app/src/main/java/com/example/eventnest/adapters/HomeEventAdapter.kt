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

class HomeEventAdapter(
    private var events: List<Event> = emptyList(),
    private val onEventClick: (String) -> Unit
) : RecyclerView.Adapter<HomeEventAdapter.HomeEventViewHolder>() {

    fun submitList(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeEventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_event_card, parent, false)
        return HomeEventViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeEventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class HomeEventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivBanner: ImageView = itemView.findViewById(R.id.ivHomeEventBanner)
        private val tvName: TextView = itemView.findViewById(R.id.tvHomeEventName)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvHomeEventDateTime)
        private val tvVenue: TextView = itemView.findViewById(R.id.tvHomeEventVenue)
        private val tvSeatsChip: TextView = itemView.findViewById(R.id.tvHomeEventSeatsChip)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvHomeEventPrice)

        fun bind(event: Event) {
            tvName.text = event.eventName

            // Date + Time on one line
            val dateTimeText = when {
                event.date.isNotBlank() && event.time.isNotBlank() -> "${event.date} • ${event.time}"
                event.date.isNotBlank() -> event.date
                event.time.isNotBlank() -> event.time
                else -> "Date & Time TBA"
            }
            tvDateTime.text = dateTimeText

            tvVenue.text = event.venue.ifBlank { "Venue TBA" }

            // Price formatted with formatCurrency() -> ₹
            val minP = event.minPrice
            val maxP = event.maxPrice
            tvPrice.text = when {
                minP <= 0.0 -> itemView.context.getString(R.string.price_free)
                minP == maxP -> formatCurrency(minP)
                else -> "From ${formatCurrency(minP)}"
            }

            // Seats Left Chip styling
            val availSeats = event.totalAvailableSeats
            when {
                availSeats <= 0 -> {
                    // Sold out: error color
                    tvSeatsChip.text = itemView.context.getString(R.string.sold_out)
                    tvSeatsChip.setBackgroundResource(R.drawable.bg_badge_error)
                    tvSeatsChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.error))
                }
                availSeats < 10 -> {
                    // < 10 seats: warning color
                    tvSeatsChip.text = itemView.context.getString(R.string.seats_left_format, availSeats)
                    tvSeatsChip.setBackgroundResource(R.drawable.bg_badge_warning)
                    tvSeatsChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.warning))
                }
                else -> {
                    // Normal availability
                    tvSeatsChip.text = itemView.context.getString(R.string.seats_left_format, availSeats)
                    tvSeatsChip.setBackgroundResource(R.drawable.bg_badge_seats)
                    tvSeatsChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                }
            }

            // Banner Image
            if (event.bannerImageUrl.isNotBlank()) {
                Glide.with(itemView.context)
                    .load(event.bannerImageUrl)
                    .placeholder(R.drawable.ic_event_placeholder)
                    .error(R.drawable.ic_event_placeholder)
                    .centerCrop()
                    .into(ivBanner)
            } else {
                ivBanner.setImageResource(R.drawable.ic_event_placeholder)
            }

            // Card click (still tappable even if sold out)
            itemView.setOnClickListener {
                onEventClick(event.eventId)
            }
        }
    }
}
