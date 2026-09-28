package com.opendronediary.service

import com.opendronediary.model.FlightLog
import com.opendronediary.repository.AircraftRepository
import com.opendronediary.repository.FlightLogRepository
import org.jetbrains.exposed.sql.transactions.transaction
import utils.FlightHours
import utils.FlightTimeCalculator

class FlightLogService(
    private val repository: FlightLogRepository,
    private val aircraftRepository: AircraftRepository? = null
) {
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
        
        val withDuration = flightLog.copy(totalFlightTime = finalTotalFlightTime)
        val cumulative = cumulativeMinutes(withDuration)
        return if (cumulative == null) withDuration else withDuration.copy(cumulativeFlightMinutes = cumulative)
    }

    private fun cumulativeMinutes(flightLog: FlightLog): Int? {
        val aircraftId = flightLog.aircraftId ?: return null
        val aircraftRepo = aircraftRepository ?: return null
        return transaction {
            val aircraft = aircraftRepo.getByIdAndUserId(aircraftId, flightLog.userId) ?: return@transaction null
            val others = repository.getAllByUserId(flightLog.userId)
                .filter { it.aircraftId == aircraftId && it.id != flightLog.id }
            val thisMinutes = FlightHours.flightMinutes(flightLog)
            FlightHours.snapshotMinutes(
                aircraft.initialTotalMinutes,
                others,
                thisMinutes,
                flightLog.flightDate,
                flightLog.id
            )
        }
    }
}

