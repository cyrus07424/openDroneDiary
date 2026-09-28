package com.example

import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.opendronediary.repository.FlightLogRepository
import com.opendronediary.repository.DailyInspectionRecordRepository
import com.opendronediary.repository.MaintenanceInspectionRecordRepository
import com.opendronediary.repository.UserRepository
import com.opendronediary.repository.AircraftRepository
import com.opendronediary.repository.PilotRepository
import com.opendronediary.service.AircraftService
import com.opendronediary.service.FlightLogService
import com.opendronediary.service.DailyInspectionRecordService
import com.opendronediary.service.MaintenanceInspectionRecordService
import com.opendronediary.service.UserService
import com.opendronediary.service.PilotService
import com.opendronediary.service.EmailService
import com.opendronediary.service.SlackService
import com.opendronediary.service.CaptchaService
import routing.configureTopAndAuthRouting
import routing.configureFlightLogRouting
import routing.configureDailyInspectionRouting
import routing.configureMaintenanceInspectionRouting
import routing.configureAircraftRouting
import routing.configureDashboardRouting
import routing.configurePilotRouting

fun Application.configureRouting() {
    val flightLogRepository = FlightLogRepository()
    val aircraftRepository = AircraftRepository()
    val aircraftService = AircraftService(aircraftRepository)
    val flightLogService = FlightLogService(flightLogRepository, aircraftRepository)
    val dailyInspectionRecordRepository = DailyInspectionRecordRepository()
    val dailyInspectionRecordService = DailyInspectionRecordService(dailyInspectionRecordRepository)
    val maintenanceInspectionRecordRepository = MaintenanceInspectionRecordRepository()
    val maintenanceInspectionRecordService = MaintenanceInspectionRecordService(maintenanceInspectionRecordRepository)
    val userRepository = UserRepository()
    val userService = UserService(userRepository)
    val pilotRepository = PilotRepository()
    val pilotService = PilotService(pilotRepository)
    val emailService = EmailService()
    val slackService = SlackService()
    val captchaService = CaptchaService()
    
    routing {
        configureTopAndAuthRouting(userService, emailService, slackService, captchaService)
        configureDashboardRouting(aircraftService, flightLogService, dailyInspectionRecordService, maintenanceInspectionRecordService)
        configureAircraftRouting(aircraftService, flightLogService, dailyInspectionRecordService, maintenanceInspectionRecordService)
        configureFlightLogRouting(flightLogService, slackService, pilotService, aircraftService)
        configureDailyInspectionRouting(dailyInspectionRecordService, slackService, aircraftService, flightLogService)
        configureMaintenanceInspectionRouting(maintenanceInspectionRecordService, slackService, aircraftService, flightLogService)
        configurePilotRouting(pilotService)
    }
}