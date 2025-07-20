package utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlightTimeCalculatorTest {

    @Test
    fun testCalculateFlightDurationNormalFlight() {
        // Normal same-day flight
        val result = FlightTimeCalculator.calculateFlightDuration("10:30", "11:45")
        assertEquals("1時間15分", result)
    }
    
    @Test
    fun testCalculateFlightDurationExactHour() {
        val result = FlightTimeCalculator.calculateFlightDuration("10:00", "12:00")
        assertEquals("2時間", result)
    }
    
    @Test
    fun testCalculateFlightDurationMinutesOnly() {
        val result = FlightTimeCalculator.calculateFlightDuration("10:30", "10:45")
        assertEquals("15分", result)
    }
    
    @Test
    fun testCalculateFlightDurationCrossesMiddnight() {
        // Flight that crosses midnight
        val result = FlightTimeCalculator.calculateFlightDuration("23:30", "01:30")
        assertEquals("2時間", result)
    }
    
    @Test
    fun testCalculateFlightDurationZeroMinutes() {
        val result = FlightTimeCalculator.calculateFlightDuration("10:30", "10:30")
        assertEquals("0分", result)
    }
    
    @Test
    fun testCalculateFlightDurationInvalidInput() {
        // Null inputs
        assertNull(FlightTimeCalculator.calculateFlightDuration(null, "10:30"))
        assertNull(FlightTimeCalculator.calculateFlightDuration("10:30", null))
        assertNull(FlightTimeCalculator.calculateFlightDuration(null, null))
        
        // Empty inputs
        assertNull(FlightTimeCalculator.calculateFlightDuration("", "10:30"))
        assertNull(FlightTimeCalculator.calculateFlightDuration("10:30", ""))
        
        // Invalid format
        assertNull(FlightTimeCalculator.calculateFlightDuration("25:00", "10:30"))
        assertNull(FlightTimeCalculator.calculateFlightDuration("10:30", "25:00"))
        assertNull(FlightTimeCalculator.calculateFlightDuration("abc", "10:30"))
    }
    
    @Test
    fun testIsValidTimeFormat() {
        // Valid formats
        assertTrue(FlightTimeCalculator.isValidTimeFormat("10:30"))
        assertTrue(FlightTimeCalculator.isValidTimeFormat("00:00"))
        assertTrue(FlightTimeCalculator.isValidTimeFormat("23:59"))
        assertTrue(FlightTimeCalculator.isValidTimeFormat("09:05"))
        
        // Invalid formats
        assertFalse(FlightTimeCalculator.isValidTimeFormat(null))
        assertFalse(FlightTimeCalculator.isValidTimeFormat(""))
        assertFalse(FlightTimeCalculator.isValidTimeFormat("25:00"))
        assertFalse(FlightTimeCalculator.isValidTimeFormat("10:60"))
        assertFalse(FlightTimeCalculator.isValidTimeFormat("abc"))
        assertFalse(FlightTimeCalculator.isValidTimeFormat("10-30"))
        assertFalse(FlightTimeCalculator.isValidTimeFormat("10:3"))
    }
}