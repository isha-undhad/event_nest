package com.example.eventnest.models

import java.io.Serializable

data class TicketTier(
    var tierId: String = "",
    var tierName: String = "",
    var price: Double = 0.0,
    var totalSeats: Int = 0,
    var availableSeats: Int = 0
) : Serializable
