package com.example.eventnest.utils

object EventSeatHelper {

    /**
     * Calculates the number of tickets already booked/sold for an event.
     * bookedSeats = totalSeats - availableSeats
     */
    fun calculateBookedSeats(totalSeats: Int, availableSeats: Int): Int {
        return (totalSeats - availableSeats).coerceAtLeast(0)
    }

    /**
     * In edit mode, when totalSeats is updated:
     * If totalSeats is increased, availableSeats increases by the same delta.
     * If totalSeats is decreased (down to bookedSeats), availableSeats decreases accordingly.
     * In both cases: newAvailableSeats = newTotalSeats - bookedSeats.
     */
    fun calculateNewAvailableSeats(newTotalSeats: Int, bookedSeats: Int): Int {
        return (newTotalSeats - bookedSeats).coerceAtLeast(0)
    }

    /**
     * Validates whether the new total seats is valid:
     * Must be greater than 0 and cannot be reduced below already booked tickets.
     */
    fun validateTotalSeats(newTotalSeats: Int, bookedSeats: Int): SeatValidationResult {
        return when {
            newTotalSeats <= 0 -> SeatValidationResult.Invalid("Total seats must be greater than 0")
            newTotalSeats < bookedSeats -> SeatValidationResult.Invalid(
                "Total seats ($newTotalSeats) cannot be less than already booked tickets ($bookedSeats)"
            )
            else -> SeatValidationResult.Valid(calculateNewAvailableSeats(newTotalSeats, bookedSeats))
        }
    }
}

sealed class SeatValidationResult {
    data class Valid(val newAvailableSeats: Int) : SeatValidationResult()
    data class Invalid(val errorMessage: String) : SeatValidationResult()
}
