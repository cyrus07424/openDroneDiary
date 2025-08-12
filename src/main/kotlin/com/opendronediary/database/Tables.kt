package com.opendronediary.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val email = varchar("email", 255).uniqueIndex()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}

object FlightLogs : Table() {
    val id = integer("id").autoIncrement()
    val flightDate = varchar("flight_date", 20)
    val takeoffLandingLocation = varchar("takeoff_landing_location", 255).nullable() // Keep for backward compatibility
    val takeoffLandingTime = varchar("takeoff_landing_time", 20).nullable() // Keep for backward compatibility
    val flightDuration = varchar("flight_duration", 20).nullable() // Make nullable
    val pilotName = varchar("pilot_name", 100) // Keep for backward compatibility and free text entry
    val pilotId = integer("pilot_id").references(Pilots.id).nullable() // Reference to registered pilot
    val issuesAndResponses = varchar("issues_and_responses", 1000).nullable()
    val userId = integer("user_id").references(Users.id)
    
    // New fields for enhanced flight logging
    val takeoffLocation = varchar("takeoff_location", 255).nullable()
    val landingLocation = varchar("landing_location", 255).nullable()
    val takeoffTime = varchar("takeoff_time", 20).nullable()
    val landingTime = varchar("landing_time", 20).nullable()
    val flightSummary = varchar("flight_summary", 1000).nullable()
    val totalFlightTime = varchar("total_flight_time", 20).nullable()
    
    // Coordinate and input type fields
    val takeoffInputType = varchar("takeoff_input_type", 20).default("text")
    val landingInputType = varchar("landing_input_type", 20).default("text")
    val takeoffLatitude = decimal("takeoff_latitude", 10, 8).nullable()
    val takeoffLongitude = decimal("takeoff_longitude", 11, 8).nullable()
    val landingLatitude = decimal("landing_latitude", 10, 8).nullable()
    val landingLongitude = decimal("landing_longitude", 11, 8).nullable()
    
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}

object DailyInspectionRecords : Table() {
    val id = integer("id").autoIncrement()
    val inspectionDate = varchar("inspection_date", 20)
    val location = varchar("location", 255)
    val inspectorName = varchar("inspector_name", 100)
    val inspectionResult = varchar("inspection_result", 1000)
    val userId = integer("user_id").references(Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}

object MaintenanceInspectionRecords : Table() {
    val id = integer("id").autoIncrement()
    val inspectionDate = varchar("inspection_date", 20)
    val location = varchar("location", 255)
    val inspectorName = varchar("inspector_name", 100)
    val contentAndReason = varchar("content_and_reason", 1000)
    val userId = integer("user_id").references(Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}

object Pilots : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100) // パイロット氏名
    val userId = integer("user_id").references(Users.id) // 登録したユーザーID
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}

object PasswordResetTokens : Table() {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 255)
    val token = varchar("token", 255).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val used = bool("used").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}