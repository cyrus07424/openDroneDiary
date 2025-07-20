package com.opendronediary.service

import com.opendronediary.model.FlightLog
import com.opendronediary.repository.FlightLogRepository
import utils.FlightTimeCalculator

class FlightLogService(private val repository: FlightLogRepository) {
    fun getAllByUserId(userId: Int): List<FlightLog> = repository.getAllByUserId(userId)
    fun getByIdAndUserId(id: Int, userId: Int): FlightLog? = repository.getByIdAndUserId(id, userId)
    
    fun add(flightLog: FlightLog): FlightLog {
        val enhancedFlightLog = enhanceFlightLogWithCalculations(flightLog)
        return repository.add(enhancedFlightLog)
    }
    
    fun update(id: Int, flightLog: FlightLog, userId: Int): Boolean {
        val enhancedFlightLog = enhanceFlightLogWithCalculations(flightLog)
        return repository.update(id, enhancedFlightLog, userId)
    }
    
    fun delete(id: Int, userId: Int): Boolean = repository.delete(id, userId)
    
    private fun enhanceFlightLogWithCalculations(flightLog: FlightLog): FlightLog {
        // Calculate total flight time if takeoff and landing times are provided
        val calculatedFlightTime = if (!flightLog.takeoffTime.isNullOrEmpty() && !flightLog.landingTime.isNullOrEmpty()) {
            FlightTimeCalculator.calculateFlightDuration(flightLog.takeoffTime, flightLog.landingTime)
        } else null
        
        // Use calculated time if available and no manual total flight time was provided
        val finalTotalFlightTime = when {
            !flightLog.totalFlightTime.isNullOrEmpty() -> flightLog.totalFlightTime // Use manually entered value
            calculatedFlightTime != null -> calculatedFlightTime // Use calculated value
            else -> flightLog.totalFlightTime // Keep original (which may be null)
        }
        
        return flightLog.copy(totalFlightTime = finalTotalFlightTime)
    }
}

