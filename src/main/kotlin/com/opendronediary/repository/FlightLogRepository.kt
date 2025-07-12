package com.opendronediary.repository

import com.opendronediary.model.FlightLog
import com.opendronediary.database.FlightLogs
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

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
                    userId = it[FlightLogs.userId]
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
                    userId = it[FlightLogs.userId]
                )
            }
            .singleOrNull()
    }

    fun add(flightLog: FlightLog): FlightLog = transaction {
        val insertedId = FlightLogs.insert {
            it[flightDate] = flightLog.flightDate
            it[takeoffLandingLocation] = flightLog.takeoffLandingLocation
            it[takeoffLandingTime] = flightLog.takeoffLandingTime
            it[flightDuration] = flightLog.flightDuration
            it[pilotName] = flightLog.pilotName
            it[issuesAndResponses] = flightLog.issuesAndResponses
            it[userId] = flightLog.userId
        } get FlightLogs.id
        flightLog.copy(id = insertedId)
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

