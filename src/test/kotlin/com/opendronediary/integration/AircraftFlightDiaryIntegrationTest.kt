package com.opendronediary.integration

import com.opendronediary.database.DatabaseConfig
import com.opendronediary.model.Aircraft
import com.opendronediary.model.FlightLog
import com.opendronediary.model.User
import com.opendronediary.repository.AircraftRepository
import com.opendronediary.repository.FlightLogRepository
import com.opendronediary.repository.UserRepository
import com.opendronediary.service.AircraftService
import com.opendronediary.service.DuplicateRegistrationException
import com.opendronediary.service.FlightLogService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class AircraftFlightDiaryIntegrationTest {
    init {
        DatabaseConfig.initDatabase()
    }

    @Test
    fun aircraftRegistrationIsNormalizedAndCumulativeHoursAreSnapshotted() {
        val stamp = System.nanoTime()
        val user = UserRepository().add(
            User(
                id = 0,
                username = "aircraft_$stamp",
                passwordHash = "hashed",
                email = "aircraft_$stamp@example.com"
            )
        )
        val aircraftService = AircraftService(AircraftRepository())
        val flightLogService = FlightLogService(FlightLogRepository(), AircraftRepository())

        val aircraft = aircraftService.add(
            Aircraft(
                id = 0,
                registrationSymbol = " ju0001 ",
                userId = user.id,
                manufacturer = " Open ",
                initialTotalMinutes = 30,
                maintenanceIntervalHours = 0
            )
        )
        assertEquals("JU0001", aircraft.registrationSymbol)
        assertEquals("Open", aircraft.manufacturer)
        assertEquals(1, aircraft.maintenanceIntervalHours)

        assertFailsWith<DuplicateRegistrationException> {
            aircraftService.add(
                Aircraft(id = 0, registrationSymbol = "ju0001", userId = user.id)
            )
        }

        val first = flightLogService.add(
            FlightLog(
                id = 0,
                flightDate = "2026-04-01",
                pilotName = "山田",
                userId = user.id,
                aircraftId = aircraft.id,
                takeoffTime = "10:00",
                landingTime = "10:45"
            )
        )
        assertEquals("45分", first.totalFlightTime)
        assertEquals(75, first.cumulativeFlightMinutes)

        val second = flightLogService.add(
            FlightLog(
                id = 0,
                flightDate = "2026-04-02",
                pilotName = "山田",
                userId = user.id,
                aircraftId = aircraft.id,
                totalFlightTime = "15分"
            )
        )
        assertEquals(90, second.cumulativeFlightMinutes)

        val stored = flightLogService.getByIdAndUserId(second.id, user.id)
        assertNotNull(stored)
        assertEquals(90, stored.cumulativeFlightMinutes)
    }
}
