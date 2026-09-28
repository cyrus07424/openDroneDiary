package com.opendronediary.service

import com.opendronediary.model.Aircraft
import com.opendronediary.repository.AircraftRepository
import org.jetbrains.exposed.sql.transactions.transaction

class DuplicateRegistrationException(message: String) : IllegalArgumentException(message)

class AircraftService(private val aircraftRepository: AircraftRepository) {
    fun getAllByUserId(userId: Int): List<Aircraft> = transaction {
        aircraftRepository.getAllByUserId(userId)
    }

    fun getByIdAndUserId(id: Int, userId: Int): Aircraft? = transaction {
        aircraftRepository.getByIdAndUserId(id, userId)
    }

    fun add(aircraft: Aircraft): Aircraft = transaction {
        val normalized = aircraft.normalize()
        val existing = aircraftRepository.findByRegistration(normalized.userId, normalized.registrationSymbol)
        if (existing != null) {
            throw DuplicateRegistrationException("同じ登録記号の機体が既に登録されています")
        }
        aircraftRepository.insert(normalized)
    }

    fun update(id: Int, aircraft: Aircraft, userId: Int): Boolean = transaction {
        val normalized = aircraft.normalize()
        val existing = aircraftRepository.findByRegistration(userId, normalized.registrationSymbol)
        if (existing != null && existing.id != id) {
            throw DuplicateRegistrationException("同じ登録記号の機体が既に登録されています")
        }
        aircraftRepository.update(id, normalized, userId)
    }

    fun delete(id: Int, userId: Int): Boolean = transaction {
        aircraftRepository.delete(id, userId)
    }

    private fun Aircraft.normalize(): Aircraft {
        return copy(
            registrationSymbol = registrationSymbol.trim().uppercase(),
            manufacturer = manufacturer?.trim()?.ifEmpty { null },
            modelName = modelName?.trim()?.ifEmpty { null },
            serialNumber = serialNumber?.trim()?.ifEmpty { null },
            category = category?.trim()?.ifEmpty { null },
            typeCertificateNumber = typeCertificateNumber?.trim()?.ifEmpty { null },
            aircraftCertificateClass = aircraftCertificateClass?.trim()?.ifEmpty { null },
            aircraftCertificateNumber = aircraftCertificateNumber?.trim()?.ifEmpty { null },
            notes = notes?.trim()?.ifEmpty { null },
            initialTotalMinutes = initialTotalMinutes.coerceAtLeast(0),
            maintenanceIntervalHours = maintenanceIntervalHours.coerceIn(1, 500)
        )
    }
}
