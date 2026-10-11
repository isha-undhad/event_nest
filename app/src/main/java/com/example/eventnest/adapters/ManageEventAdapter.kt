package com.example.eventnest.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eventnest.R
import com.example.eventnest.models.Event
import com.example.eventnest.utils.formatCurrency

class ManageEventAdapter(
    private val onEditClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit,
    private val onItemClick: ((Event) -> Unit)? = null
) : ListAdapter<Event, ManageEventAdapter.ManageEventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageEventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_manage_event, parent, false)
        return ManageEventViewHolder(view)
    }

    override fun onBindViewHolder(holder: ManageEventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ManageEventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumb: ImageView = itemView.findViewById(R.id.ivManageEventThumb)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvManageEventStatus)
        private val tvSeats: TextView = itemView.findViewById(R.id.tvManageEventSeats)
        private val tvName: TextView = itemView.findViewById(R.id.tvManageEventName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvManageEventDate)
        private val tvVenue: TextView = itemView.findViewById(R.id.tvManageEventVenue)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvManageEventPrice)
        private val btnEdit: View = itemView.findViewById(R.id.btnEditEvent)
        private val btnDelete: View = itemView.findViewById(R.id.btnDeleteEvent)

        fun bind(event: Event) {
            val context = itemView.context
            tvName.text = event.eventName

            // Date and Time
            val dateTime = when {
                event.date.isNotBlank() && event.time.isNotBlank() -> "${event.date} • ${event.time}"
                event.date.isNotBlank() -> event.date
                event.time.isNotBlank() -> event.time
                else -> "Date & Time TBA"
            }
            tvDate.text = dateTime

            tvVenue.text = event.venue.ifBlank { "Venue TBA" }

            // Price formatted with Rupee
            val minP = event.minPrice
            val maxP = event.maxPrice
            tvPrice.text = when {
                minP <= 0.0 -> context.getString(R.string.price_free)
                minP == maxP -> formatCurrency(minP)
                else -> "From ${formatCurrency(minP)}"
            }

            // Status styling
            val isCancelled = event.status.equals("cancelled", ignoreCase = true)
            if (isCancelled) {
                tvStatus.text = "CANCELLED"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_error)
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error))

                // Dim delete button for already cancelled events
                btnDelete.isEnabled = false
                btnDelete.alpha = 0.4f
            } else {
                tvStatus.text = "ACTIVE"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_success)
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success))

                btnDelete.isEnabled = true
                btnDelete.alpha = 1.0f
            }

            // Seats breakdown styling
            val total = event.totalCapacitySeats
            val avail = event.totalAvailableSeats
            when {
                avail <= 0 -> {
                    tvSeats.text = "Sold out ($avail/$total)"
                    tvSeats.setBackgroundResource(R.drawable.bg_badge_error)
                    tvSeats.setTextColor(ContextCompat.getColor(context, R.color.error))
                }
                avail < 10 -> {
                    tvSeats.text = "$avail / $total seats"
                    tvSeats.setBackgroundResource(R.drawable.bg_badge_warning)
                    tvSeats.setTextColor(ContextCompat.getColor(context, R.color.warning))
                }
                else -> {
                    tvSeats.text = "$avail / $total seats"
                    tvSeats.setBackgroundResource(R.drawable.bg_badge_seats)
                    tvSeats.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
            }

            // Thumbnail loading via Glide
            if (event.bannerImageUrl.isNotBlank()) {
                Glide.with(context)
                    .load(event.bannerImageUrl)
                    .placeholder(R.drawable.ic_event_placeholder)
                    .error(R.drawable.ic_event_placeholder)
                    .centerCrop()
                    .into(ivThumb)
            } else {
                ivThumb.setImageResource(R.drawable.ic_event_placeholder)
            }

            // Clicks
            btnEdit.setOnClickListener {
                onEditClick(event)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(event)
            }

            itemView.setOnClickListener {
                onItemClick?.invoke(event)
            }
        }
    }

    private class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.eventId == newItem.eventId
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}
