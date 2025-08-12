package com.opendronediary.integration

import com.opendronediary.database.DatabaseConfig
import com.opendronediary.model.Pilot
import com.opendronediary.model.FlightLog
import com.opendronediary.model.User
import com.opendronediary.repository.PilotRepository
import com.opendronediary.repository.FlightLogRepository
import com.opendronediary.repository.UserRepository
import com.opendronediary.service.PilotService
import com.opendronediary.service.FlightLogService
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PilotManagementIntegrationTest {
    
    init {
        DatabaseConfig.initDatabase()
    }
    
    @Test
    fun testPilotCRUDOperations() {
        val userRepository = UserRepository()
        val pilotRepository = PilotRepository()
        val pilotService = PilotService(pilotRepository)
        
        transaction {
            // Create a test user
            val testUser = userRepository.add(User(
                id = 0,
                username = "testuser_crud_${System.currentTimeMillis()}",
                passwordHash = "hashedpassword",
                email = "test_crud_${System.currentTimeMillis()}@example.com"
            ))
            
            // Test pilot creation
            val pilot = pilotService.add(Pilot(
                id = 0,
                name = "Test Pilot",
                userId = testUser.id
            ))
            
            assertNotNull(pilot)
            assertTrue(pilot.id > 0)
            assertEquals("Test Pilot", pilot.name)
            assertEquals(testUser.id, pilot.userId)
            
            // Test pilot retrieval
            val retrievedPilot = pilotService.getByIdAndUserId(pilot.id, testUser.id)
            assertNotNull(retrievedPilot)
            assertEquals(pilot.name, retrievedPilot.name)
            
            // Test pilots list
            val pilots = pilotService.getAllByUserId(testUser.id)
            assertTrue(pilots.isNotEmpty())
            assertEquals(1, pilots.size)
            assertEquals("Test Pilot", pilots[0].name)
            
            // Test pilot update
            val updated = pilotService.update(pilot.id, pilot.copy(name = "Updated Pilot"), testUser.id)
            assertTrue(updated)
            
            val updatedPilot = pilotService.getByIdAndUserId(pilot.id, testUser.id)
            assertNotNull(updatedPilot)
            assertEquals("Updated Pilot", updatedPilot.name)
            
            // Test pilot deletion
            val deleted = pilotService.delete(pilot.id, testUser.id)
            assertTrue(deleted)
            
            val deletedPilot = pilotService.getByIdAndUserId(pilot.id, testUser.id)
            assertEquals(null, deletedPilot)
        }
    }
    
    @Test
    fun testFlightLogWithPilotIntegration() {
        val userRepository = UserRepository()
        val pilotRepository = PilotRepository()
        val flightLogRepository = FlightLogRepository()
        val pilotService = PilotService(pilotRepository)
        val flightLogService = FlightLogService(flightLogRepository)
        
        transaction {
            // Create a test user
            val testUser = userRepository.add(User(
                id = 0,
                username = "testuser_flight_${System.currentTimeMillis()}",
                passwordHash = "hashedpassword",
                email = "test_flight_${System.currentTimeMillis()}@example.com"
            ))
            
            // Create a test pilot
            val pilot = pilotService.add(Pilot(
                id = 0,
                name = "Test Pilot",
                userId = testUser.id
            ))
            
            // Test flight log with registered pilot
            val flightLogWithPilot = flightLogService.add(FlightLog(
                id = 0,
                flightDate = "2023-12-01",
                pilotName = pilot.name,
                pilotId = pilot.id,
                userId = testUser.id,
                takeoffLocation = "Location A",
                landingLocation = "Location B",
                takeoffTime = "10:00",
                landingTime = "11:00",
                totalFlightTime = "1時間"
            ))
            
            assertNotNull(flightLogWithPilot)
            assertEquals(pilot.name, flightLogWithPilot.pilotName)
            assertEquals(pilot.id, flightLogWithPilot.pilotId)
            
            // Test flight log with free text pilot (backward compatibility)
            val flightLogWithText = flightLogService.add(FlightLog(
                id = 0,
                flightDate = "2023-12-02",
                pilotName = "Free Text Pilot",
                pilotId = null,
                userId = testUser.id,
                takeoffLocation = "Location C",
                landingLocation = "Location D",
                takeoffTime = "14:00",
                landingTime = "15:00",
                totalFlightTime = "1時間"
            ))
            
            assertNotNull(flightLogWithText)
            assertEquals("Free Text Pilot", flightLogWithText.pilotName)
            assertEquals(null, flightLogWithText.pilotId)
            
            // Verify both flight logs are stored correctly
            val flightLogs = flightLogService.getAllByUserId(testUser.id)
            assertEquals(2, flightLogs.size)
            
            // Find and verify the pilot-linked flight log
            val pilotFlightLog = flightLogs.find { it.pilotId != null }
            assertNotNull(pilotFlightLog)
            assertEquals(pilot.id, pilotFlightLog.pilotId)
            assertEquals(pilot.name, pilotFlightLog.pilotName)
            
            // Find and verify the free text flight log
            val textFlightLog = flightLogs.find { it.pilotId == null }
            assertNotNull(textFlightLog)
            assertEquals(null, textFlightLog.pilotId)
            assertEquals("Free Text Pilot", textFlightLog.pilotName)
        }
    }
}