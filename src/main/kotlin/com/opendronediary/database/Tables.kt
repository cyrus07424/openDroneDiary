package com.opendronediary.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val email = varchar("email", 255).nullable()
    
    override val primaryKey = PrimaryKey(id)
}

object FlightLogs : Table() {
    val id = integer("id").autoIncrement()
    val flightDate = varchar("flight_date", 20)
    val takeoffLandingLocation = varchar("takeoff_landing_location", 255)
    val takeoffLandingTime = varchar("takeoff_landing_time", 20)
    val flightDuration = varchar("flight_duration", 20)
    val pilotName = varchar("pilot_name", 100)
    val issuesAndResponses = varchar("issues_and_responses", 1000).nullable()
    val userId = integer("user_id").references(Users.id)
    
    override val primaryKey = PrimaryKey(id)
}

object DailyInspectionRecords : Table() {
    val id = integer("id").autoIncrement()
    val inspectionDate = varchar("inspection_date", 20)
    val location = varchar("location", 255)
    val inspectorName = varchar("inspector_name", 100)
    val inspectionResult = varchar("inspection_result", 1000)
    val userId = integer("user_id").references(Users.id)
    
    override val primaryKey = PrimaryKey(id)
}

object MaintenanceInspectionRecords : Table() {
    val id = integer("id").autoIncrement()
    val inspectionDate = varchar("inspection_date", 20)
    val location = varchar("location", 255)
    val inspectorName = varchar("inspector_name", 100)
    val contentAndReason = varchar("content_and_reason", 1000)
    val userId = integer("user_id").references(Users.id)
    
    override val primaryKey = PrimaryKey(id)
}

object PasswordResetTokens : Table() {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 255)
    val token = varchar("token", 255).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val used = bool("used").default(false)
    
    override val primaryKey = PrimaryKey(id)
}