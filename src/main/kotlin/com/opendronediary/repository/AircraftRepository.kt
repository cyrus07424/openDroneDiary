package com.opendronediary.repository

import com.opendronediary.database.Aircrafts
import com.opendronediary.model.Aircraft
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

class AircraftRepository {
    fun getAllByUserId(userId: Int): List<Aircraft> {
        return Aircrafts.selectAll().where { Aircrafts.userId eq userId }
            .orderBy(Aircrafts.registrationSymbol)
            .map { resultRowToAircraft(it) }
    }

    fun getByIdAndUserId(id: Int, userId: Int): Aircraft? {
        return Aircrafts.selectAll().where { (Aircrafts.id eq id) and (Aircrafts.userId eq userId) }
            .map { resultRowToAircraft(it) }
            .firstOrNull()
    }

    fun findByRegistration(userId: Int, registrationSymbol: String): Aircraft? {
        return Aircrafts.selectAll()
            .where { (Aircrafts.userId eq userId) and (Aircrafts.registrationSymbol eq registrationSymbol) }
            .map { resultRowToAircraft(it) }
            .firstOrNull()
    }

    fun insert(aircraft: Aircraft): Aircraft {
        val now = LocalDateTime.now()
        val insertedId = Aircrafts.insert {
            it[registrationSymbol] = aircraft.registrationSymbol
            it[userId] = aircraft.userId
            it[manufacturer] = aircraft.manufacturer
            it[modelName] = aircraft.modelName
            it[serialNumber] = aircraft.serialNumber
            it[category] = aircraft.category
            it[typeCertificateNumber] = aircraft.typeCertificateNumber
            it[aircraftCertificateClass] = aircraft.aircraftCertificateClass
            it[aircraftCertificateNumber] = aircraft.aircraftCertificateNumber
            it[initialTotalMinutes] = aircraft.initialTotalMinutes
            it[maintenanceIntervalHours] = aircraft.maintenanceIntervalHours
            it[notes] = aircraft.notes
            it[createdAt] = now
            it[updatedAt] = now
        } get Aircrafts.id
        return aircraft.copy(id = insertedId, createdAt = now, updatedAt = now)
    }

    fun update(id: Int, aircraft: Aircraft, userId: Int): Boolean {
        val updatedRows = Aircrafts.update({ (Aircrafts.id eq id) and (Aircrafts.userId eq userId) }) {
            it[registrationSymbol] = aircraft.registrationSymbol
            it[manufacturer] = aircraft.manufacturer
            it[modelName] = aircraft.modelName
            it[serialNumber] = aircraft.serialNumber
            it[category] = aircraft.category
            it[typeCertificateNumber] = aircraft.typeCertificateNumber
            it[aircraftCertificateClass] = aircraft.aircraftCertificateClass
            it[aircraftCertificateNumber] = aircraft.aircraftCertificateNumber
            it[initialTotalMinutes] = aircraft.initialTotalMinutes
            it[maintenanceIntervalHours] = aircraft.maintenanceIntervalHours
            it[notes] = aircraft.notes
            it[updatedAt] = LocalDateTime.now()
        }
        return updatedRows > 0
    }

    fun delete(id: Int, userId: Int): Boolean {
        val deletedRows = Aircrafts.deleteWhere { (Aircrafts.id eq id) and (Aircrafts.userId eq userId) }
        return deletedRows > 0
    }

    private fun resultRowToAircraft(row: ResultRow): Aircraft {
        return Aircraft(
            id = row[Aircrafts.id],
            registrationSymbol = row[Aircrafts.registrationSymbol],
            userId = row[Aircrafts.userId],
            manufacturer = row[Aircrafts.manufacturer],
            modelName = row[Aircrafts.modelName],
            serialNumber = row[Aircrafts.serialNumber],
            category = row[Aircrafts.category],
            typeCertificateNumber = row[Aircrafts.typeCertificateNumber],
            aircraftCertificateClass = row[Aircrafts.aircraftCertificateClass],
            aircraftCertificateNumber = row[Aircrafts.aircraftCertificateNumber],
            initialTotalMinutes = row[Aircrafts.initialTotalMinutes],
            maintenanceIntervalHours = row[Aircrafts.maintenanceIntervalHours],
            notes = row[Aircrafts.notes],
            createdAt = row[Aircrafts.createdAt],
            updatedAt = row[Aircrafts.updatedAt]
        )
    }
}
