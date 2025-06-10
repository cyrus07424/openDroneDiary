package com.opendronediary.repository

import com.opendronediary.model.FlightLog

class FlightLogRepository {
    private val flightLogs = mutableListOf<FlightLog>()
    private var nextId = 1

    fun getAllByUserId(userId: Int): List<FlightLog> = flightLogs.filter { it.userId == userId }

    fun getByIdAndUserId(id: Int, userId: Int): FlightLog? = flightLogs.find { it.id == id && it.userId == userId }

    fun add(flightLog: FlightLog): FlightLog {
        val newFlightLog = flightLog.copy(id = nextId++)
        flightLogs.add(newFlightLog)
        return newFlightLog
    }

    fun update(id: Int, flightLog: FlightLog, userId: Int): Boolean {
        val index = flightLogs.indexOfFirst { it.id == id && it.userId == userId }
        if (index == -1) return false
        flightLogs[index] = flightLog.copy(id = id, userId = userId)
        return true
    }

    fun delete(id: Int, userId: Int): Boolean = flightLogs.removeIf { it.id == id && it.userId == userId }
}

