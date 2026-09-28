package com.opendronediary.repository

import com.opendronediary.model.FlightLog
import com.opendronediary.database.FlightLogs
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class FlightLogRepository {
    
    fun getAllByUserId(userId: Int): List<FlightLog> = transaction {
        FlightLogs.select { FlightLogs.userId eq userId }
            .map { toFlightLog(it) }
    }

    private fun toFlightLog(row: ResultRow): FlightLog {
        return FlightLog(
            id = row[FlightLogs.id],
            flightDate = row[FlightLogs.flightDate],
            takeoffLandingLocation = row[FlightLogs.takeoffLandingLocation],
            takeoffLandingTime = row[FlightLogs.takeoffLandingTime],
            flightDuration = row[FlightLogs.flightDuration],
            pilotName = row[FlightLogs.pilotName],
            pilotId = row[FlightLogs.pilotId],
            issuesAndResponses = row[FlightLogs.issuesAndResponses],
            userId = row[FlightLogs.userId],
            takeoffLocation = row[FlightLogs.takeoffLocation],
            landingLocation = row[FlightLogs.landingLocation],
            takeoffTime = row[FlightLogs.takeoffTime],
            landingTime = row[FlightLogs.landingTime],
            flightSummary = row[FlightLogs.flightSummary],
            totalFlightTime = row[FlightLogs.totalFlightTime],
            takeoffInputType = row[FlightLogs.takeoffInputType],
            landingInputType = row[FlightLogs.landingInputType],
            takeoffLatitude = row[FlightLogs.takeoffLatitude],
            takeoffLongitude = row[FlightLogs.takeoffLongitude],
            landingLatitude = row[FlightLogs.landingLatitude],
            landingLongitude = row[FlightLogs.landingLongitude],
            aircraftId = row[FlightLogs.aircraftId],
            flightPurpose = row[FlightLogs.flightPurpose],
            flightRoute = row[FlightLogs.flightRoute],
            specificFlight = row[FlightLogs.specificFlight],
            safetyMatters = row[FlightLogs.safetyMatters],
            skillCertificateNumber = row[FlightLogs.skillCertificateNumber],
            permissionNumber = row[FlightLogs.permissionNumber],
            cumulativeFlightMinutes = row[FlightLogs.cumulativeFlightMinutes],
            createdAt = row[FlightLogs.createdAt],
            updatedAt = row[FlightLogs.updatedAt]
        )
    }

    fun getByIdAndUserId(id: Int, userId: Int): FlightLog? = transaction {
        FlightLogs.select { (FlightLogs.id eq id) and (FlightLogs.userId eq userId) }
            .map { toFlightLog(it) }
            .singleOrNull()
    }

    fun add(flightLog: FlightLog): FlightLog = transaction {
        val now = LocalDateTime.now()
        val insertedId = FlightLogs.insert {
            it[flightDate] = flightLog.flightDate
            it[takeoffLandingLocation] = flightLog.takeoffLandingLocation
            it[takeoffLandingTime] = flightLog.takeoffLandingTime
            it[flightDuration] = flightLog.flightDuration
            it[pilotName] = flightLog.pilotName
            it[pilotId] = flightLog.pilotId
            it[issuesAndResponses] = flightLog.issuesAndResponses
            it[userId] = flightLog.userId
            it[takeoffLocation] = flightLog.takeoffLocation
            it[landingLocation] = flightLog.landingLocation
            it[takeoffTime] = flightLog.takeoffTime
            it[landingTime] = flightLog.landingTime
            it[flightSummary] = flightLog.flightSummary
            it[totalFlightTime] = flightLog.totalFlightTime
            it[takeoffInputType] = flightLog.takeoffInputType
            it[landingInputType] = flightLog.landingInputType
            it[takeoffLatitude] = flightLog.takeoffLatitude
            it[takeoffLongitude] = flightLog.takeoffLongitude
            it[landingLatitude] = flightLog.landingLatitude
            it[landingLongitude] = flightLog.landingLongitude
            it[aircraftId] = flightLog.aircraftId
            it[flightPurpose] = flightLog.flightPurpose
            it[flightRoute] = flightLog.flightRoute
            it[specificFlight] = flightLog.specificFlight
            it[safetyMatters] = flightLog.safetyMatters
            it[skillCertificateNumber] = flightLog.skillCertificateNumber
            it[permissionNumber] = flightLog.permissionNumber
            it[cumulativeFlightMinutes] = flightLog.cumulativeFlightMinutes
            it[createdAt] = now
            it[updatedAt] = now
        } get FlightLogs.id
        flightLog.copy(id = insertedId, createdAt = now, updatedAt = now)
    }

    fun update(id: Int, flightLog: FlightLog, userId: Int): Boolean = transaction {
        val updateCount = FlightLogs.update(
            { (FlightLogs.id eq id) and (FlightLogs.userId eq userId) }
        ) {
            it[flightDate] = flightLog.flightDate
            it[takeoffLandingLocation] = flightLog.takeoffLandingLocation
            it[takeoffLandingTime] = flightLog.takeoffLandingTime
            it[flightDuration] = flightLog.flightDuration
            it[pilotName] = flightLog.pilotName
            it[pilotId] = flightLog.pilotId
            it[issuesAndResponses] = flightLog.issuesAndResponses
            it[takeoffLocation] = flightLog.takeoffLocation
            it[landingLocation] = flightLog.landingLocation
            it[takeoffTime] = flightLog.takeoffTime
            it[landingTime] = flightLog.landingTime
            it[flightSummary] = flightLog.flightSummary
            it[totalFlightTime] = flightLog.totalFlightTime
            it[takeoffInputType] = flightLog.takeoffInputType
            it[landingInputType] = flightLog.landingInputType
            it[takeoffLatitude] = flightLog.takeoffLatitude
            it[takeoffLongitude] = flightLog.takeoffLongitude
            it[landingLatitude] = flightLog.landingLatitude
            it[landingLongitude] = flightLog.landingLongitude
            it[aircraftId] = flightLog.aircraftId
            it[flightPurpose] = flightLog.flightPurpose
            it[flightRoute] = flightLog.flightRoute
            it[specificFlight] = flightLog.specificFlight
            it[safetyMatters] = flightLog.safetyMatters
            it[skillCertificateNumber] = flightLog.skillCertificateNumber
            it[permissionNumber] = flightLog.permissionNumber
            it[cumulativeFlightMinutes] = flightLog.cumulativeFlightMinutes
            it[updatedAt] = LocalDateTime.now()
        }
        updateCount > 0
    }

    fun delete(id: Int, userId: Int): Boolean = transaction {
        val deleteCount = FlightLogs.deleteWhere { 
            (FlightLogs.id eq id) and (FlightLogs.userId eq userId) 
        }
        deleteCount > 0
    }
}

