package com.opendronediary.database

import org.jetbrains.exposed.sql.ReferenceOption
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

object Aircrafts : Table() {
    val id = integer("id").autoIncrement()
    val registrationSymbol = varchar("registration_symbol", 32)
    val userId = integer("user_id").references(Users.id)
    val manufacturer = varchar("manufacturer", 100).nullable()
    val modelName = varchar("model_name", 100).nullable()
    val serialNumber = varchar("serial_number", 100).nullable()
    val category = varchar("category", 100).nullable()
    val typeCertificateNumber = varchar("type_certificate_number", 100).nullable()
    val aircraftCertificateClass = varchar("aircraft_certificate_class", 20).nullable()
    val aircraftCertificateNumber = varchar("aircraft_certificate_number", 100).nullable()
    val initialTotalMinutes = integer("initial_total_minutes").default(0)
    val maintenanceIntervalHours = integer("maintenance_interval_hours").default(20)
    val notes = varchar("notes", 1000).nullable()
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
    val aircraftId = integer("aircraft_id").references(Aircrafts.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val flightPurpose = varchar("flight_purpose", 500).nullable()
    val flightRoute = varchar("flight_route", 1000).nullable()
    val specificFlight = varchar("specific_flight", 500).nullable()
    val safetyMatters = varchar("safety_matters", 1000).nullable()
    val skillCertificateNumber = varchar("skill_certificate_number", 50).nullable()
    val permissionNumber = varchar("permission_number", 100).nullable()
    val cumulativeFlightMinutes = integer("cumulative_flight_minutes").nullable()
    
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
    val aircraftId = integer("aircraft_id").references(Aircrafts.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val airframeResult = varchar("airframe_result", 20).nullable()
    val propellerResult = varchar("propeller_result", 20).nullable()
    val frameResult = varchar("frame_result", 20).nullable()
    val communicationResult = varchar("communication_result", 20).nullable()
    val propulsionResult = varchar("propulsion_result", 20).nullable()
    val powerResult = varchar("power_result", 20).nullable()
    val automaticControlResult = varchar("automatic_control_result", 20).nullable()
    val controllerResult = varchar("controller_result", 20).nullable()
    val batteryResult = varchar("battery_result", 20).nullable()
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
    val aircraftId = integer("aircraft_id").references(Aircrafts.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val totalFlightTime = varchar("total_flight_time", 20).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}

object Pilots : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100) // パイロット氏名
    val skillCertificateNumber = varchar("skill_certificate_number", 50).nullable()
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

object UserRegistrationTokens : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50)
    val passwordHash = varchar("password_hash", 255)
    val email = varchar("email", 255)
    val token = varchar("token", 255).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val used = bool("used").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}