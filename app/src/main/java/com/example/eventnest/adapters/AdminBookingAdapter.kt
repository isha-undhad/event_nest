package com.example.eventnest.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.eventnest.R
import com.example.eventnest.models.Booking
import com.example.eventnest.utils.formatCurrency

class AdminBookingAdapter : ListAdapter<Booking, AdminBookingAdapter.AdminBookingViewHolder>(BookingDiffCallback()) {

    private val userCache = mutableMapOf<String, String>()

    fun updateUserCache(updatedMap: Map<String, String>) {
        userCache.clear()
        userCache.putAll(updatedMap)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminBookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_booking, parent, false)
        return AdminBookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminBookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdminBookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEventName: TextView = itemView.findViewById(R.id.tvAdminBookingEventName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvAdminBookingStatus)
        private val tvDate: TextView = itemView.findViewById(R.id.tvAdminBookingDate)
        private val tvBookingId: TextView = itemView.findViewById(R.id.tvAdminBookingId)
        private val tvCustomer: TextView = itemView.findViewById(R.id.tvAdminBookingCustomer)
        private val tvTickets: TextView = itemView.findViewById(R.id.tvAdminBookingTickets)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAdminBookingAmount)

        fun bind(booking: Booking) {
            val context = itemView.context
            tvEventName.text = booking.eventName.ifBlank { "Event Reservation" }
            tvDate.text = booking.bookingDate.ifBlank { "Date TBA" }

            // Booking ID snippet
            val idSnippet = if (booking.bookingId.length > 8) {
                "#${booking.bookingId.takeLast(8)}"
            } else {
                "#${booking.bookingId}"
            }
            tvBookingId.text = idSnippet

            // Ticket count
            val ticketCount = booking.numberOfTickets
            val baseTicket = "$ticketCount ${if (ticketCount == 1) "ticket" else "tickets"}"
            tvTickets.text = if (booking.tierName.isNotBlank()) "$baseTicket (${booking.tierName})" else baseTicket

            // Formatted Amount
            tvAmount.text = formatCurrency(booking.totalAmount)

            // Status Styling
            val status = booking.status.lowercase()
            when (status) {
                "confirmed" -> {
                    tvStatus.text = "CONFIRMED"
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_success)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
                    tvAmount.paintFlags = tvAmount.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                "pending" -> {
                    tvStatus.text = "PENDING"
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_warning)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning))
                    tvAmount.paintFlags = tvAmount.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                "cancelled" -> {
                    tvStatus.text = "CANCELLED"
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_error)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error))
                    // Apply strikethrough on cancelled amount
                    tvAmount.paintFlags = tvAmount.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                }
                else -> {
                    tvStatus.text = booking.status.uppercase()
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_seats)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    tvAmount.paintFlags = tvAmount.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            }

            // Customer Label from Cache or Fallback
            val cachedCustomer = userCache[booking.userId]
            if (!cachedCustomer.isNullOrBlank()) {
                tvCustomer.text = cachedCustomer
            } else if (booking.userId.isNotBlank()) {
                tvCustomer.text = "User ID: ${booking.userId.take(12)}..."
            } else {
                tvCustomer.text = "Guest User"
            }
        }
    }

    private class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem.bookingId == newItem.bookingId
        }

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem == newItem
        }
    }
}
