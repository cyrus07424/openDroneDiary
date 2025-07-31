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
            .map { 
                FlightLog(
                    id = it[FlightLogs.id],
                    flightDate = it[FlightLogs.flightDate],
                    takeoffLandingLocation = it[FlightLogs.takeoffLandingLocation],
                    takeoffLandingTime = it[FlightLogs.takeoffLandingTime],
                    flightDuration = it[FlightLogs.flightDuration],
                    pilotName = it[FlightLogs.pilotName],
                    issuesAndResponses = it[FlightLogs.issuesAndResponses],
                    userId = it[FlightLogs.userId],
                    takeoffLocation = it[FlightLogs.takeoffLocation],
                    landingLocation = it[FlightLogs.landingLocation],
                    takeoffTime = it[FlightLogs.takeoffTime],
                    landingTime = it[FlightLogs.landingTime],
                    flightSummary = it[FlightLogs.flightSummary],
                    totalFlightTime = it[FlightLogs.totalFlightTime],
                    takeoffInputType = it[FlightLogs.takeoffInputType],
                    landingInputType = it[FlightLogs.landingInputType],
                    takeoffLatitude = it[FlightLogs.takeoffLatitude],
                    takeoffLongitude = it[FlightLogs.takeoffLongitude],
                    landingLatitude = it[FlightLogs.landingLatitude],
                    landingLongitude = it[FlightLogs.landingLongitude],
                    createdAt = it[FlightLogs.createdAt],
                    updatedAt = it[FlightLogs.updatedAt]
                )
            }
    }

    fun getByIdAndUserId(id: Int, userId: Int): FlightLog? = transaction {
        FlightLogs.select { (FlightLogs.id eq id) and (FlightLogs.userId eq userId) }
            .map { 
                FlightLog(
                    id = it[FlightLogs.id],
                    flightDate = it[FlightLogs.flightDate],
                    takeoffLandingLocation = it[FlightLogs.takeoffLandingLocation],
                    takeoffLandingTime = it[FlightLogs.takeoffLandingTime],
                    flightDuration = it[FlightLogs.flightDuration],
                    pilotName = it[FlightLogs.pilotName],
                    issuesAndResponses = it[FlightLogs.issuesAndResponses],
                    userId = it[FlightLogs.userId],
                    takeoffLocation = it[FlightLogs.takeoffLocation],
                    landingLocation = it[FlightLogs.landingLocation],
                    takeoffTime = it[FlightLogs.takeoffTime],
                    landingTime = it[FlightLogs.landingTime],
                    flightSummary = it[FlightLogs.flightSummary],
                    totalFlightTime = it[FlightLogs.totalFlightTime],
                    takeoffInputType = it[FlightLogs.takeoffInputType],
                    landingInputType = it[FlightLogs.landingInputType],
                    takeoffLatitude = it[FlightLogs.takeoffLatitude],
                    takeoffLongitude = it[FlightLogs.takeoffLongitude],
                    landingLatitude = it[FlightLogs.landingLatitude],
                    landingLongitude = it[FlightLogs.landingLongitude],
                    createdAt = it[FlightLogs.createdAt],
                    updatedAt = it[FlightLogs.updatedAt]
                )
            }
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

