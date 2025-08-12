package com.opendronediary.service

import com.opendronediary.model.Pilot
import com.opendronediary.repository.PilotRepository
import org.jetbrains.exposed.sql.transactions.transaction

class PilotService(private val pilotRepository: PilotRepository) {
    fun getAllByUserId(userId: Int): List<Pilot> = transaction {
        pilotRepository.getAllByUserId(userId)
    }
    
    fun getByIdAndUserId(id: Int, userId: Int): Pilot? = transaction {
        pilotRepository.getByIdAndUserId(id, userId)
    }
    
    fun add(pilot: Pilot): Pilot = transaction {
        pilotRepository.insert(pilot)
    }
    
    fun update(id: Int, pilot: Pilot, userId: Int): Boolean = transaction {
        pilotRepository.update(id, pilot, userId)
    }
    
    fun delete(id: Int, userId: Int): Boolean = transaction {
        pilotRepository.delete(id, userId)
    }
}