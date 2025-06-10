package com.opendronediary.service

import com.opendronediary.model.FlightLog
import com.opendronediary.repository.FlightLogRepository

class FlightLogService(private val repository: FlightLogRepository) {
    fun getAllByUserId(userId: Int): List<FlightLog> = repository.getAllByUserId(userId)
    fun getByIdAndUserId(id: Int, userId: Int): FlightLog? = repository.getByIdAndUserId(id, userId)
    fun add(flightLog: FlightLog): FlightLog = repository.add(flightLog)
    fun update(id: Int, flightLog: FlightLog, userId: Int): Boolean = repository.update(id, flightLog, userId)
    fun delete(id: Int, userId: Int): Boolean = repository.delete(id, userId)
}

