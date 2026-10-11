package com.example.eventnest.models

import java.io.Serializable

data class Booking(
    var bookingId: String = "",
    var userId: String = "",
    var eventId: String = "",
    var eventName: String = "",
    var bookingDate: String = "",
    var numberOfTickets: Int = 0,
    var totalAmount: Double = 0.0,
    var status: String = "",
    var tierId: String = "",
    var tierName: String = ""
) : Serializable
