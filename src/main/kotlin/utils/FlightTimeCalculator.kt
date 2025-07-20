package utils

import java.time.LocalTime
import java.time.format.DateTimeParseException

object FlightTimeCalculator {
    
    /**
     * Calculate flight duration between takeoff and landing times
     * @param takeoffTime Time in HH:MM format
     * @param landingTime Time in HH:MM format
     * @return Flight duration as a formatted string or null if calculation failed
     */
    fun calculateFlightDuration(takeoffTime: String?, landingTime: String?): String? {
        if (takeoffTime.isNullOrEmpty() || landingTime.isNullOrEmpty()) {
            return null
        }
        
        return try {
            val takeoff = LocalTime.parse(takeoffTime)
            val landing = LocalTime.parse(landingTime)
            
            val totalMinutes = if (landing.isBefore(takeoff)) {
                // Handle next day landing (flight crosses midnight)
                // Calculate minutes from takeoff to midnight (24:00) + minutes from midnight to landing
                val minutesToMidnight = (24 * 60) - (takeoff.hour * 60 + takeoff.minute)
                val minutesFromMidnight = landing.hour * 60 + landing.minute
                minutesToMidnight + minutesFromMidnight
            } else {
                // Same day calculation
                val takeoffMinutes = takeoff.hour * 60 + takeoff.minute
                val landingMinutes = landing.hour * 60 + landing.minute
                landingMinutes - takeoffMinutes
            }
            
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            
            when {
                hours > 0 && minutes > 0 -> "${hours}時間${minutes}分"
                hours > 0 -> "${hours}時間"
                minutes > 0 -> "${minutes}分"
                else -> "0分"
            }
        } catch (e: DateTimeParseException) {
            null
        }
    }
    
    /**
     * Validate time format (HH:MM)
     * @param time Time string to validate
     * @return true if valid format, false otherwise
     */
    fun isValidTimeFormat(time: String?): Boolean {
        if (time.isNullOrEmpty()) return false
        
        return try {
            LocalTime.parse(time)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }
}