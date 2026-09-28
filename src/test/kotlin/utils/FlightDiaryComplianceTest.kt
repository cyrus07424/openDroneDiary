package utils

import com.opendronediary.model.Aircraft
import com.opendronediary.model.FlightLog
import com.opendronediary.model.MaintenanceInspectionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlightDiaryComplianceTest {
    @Test
    fun durationToMinutesParsesJapaneseAndClockFormats() {
        assertEquals(75, FlightTimeCalculator.durationToMinutes("1時間15分"))
        assertEquals(120, FlightTimeCalculator.durationToMinutes("2時間"))
        assertEquals(15, FlightTimeCalculator.durationToMinutes("15分"))
        assertEquals(90, FlightTimeCalculator.durationToMinutes("01:30"))
        assertEquals(90, FlightTimeCalculator.durationToMinutes("1:30"))
        assertEquals(90, FlightTimeCalculator.durationToMinutes("90"))
        assertNull(FlightTimeCalculator.durationToMinutes("  "))
        assertNull(FlightTimeCalculator.durationToMinutes("abc"))
        assertEquals("1時間15分", FlightTimeCalculator.formatMinutes(75))
        assertEquals("2時間", FlightTimeCalculator.formatMinutes(120))
        assertEquals("0分", FlightTimeCalculator.formatMinutes(0))
    }

    @Test
    fun specificFlightDropsNoneWhenAnotherCategoryIsSelected() {
        assertEquals("night", SpecificFlightCatalog.store(listOf("none", "night", "unknown")))
        assertEquals("none", SpecificFlightCatalog.store(listOf("none")))
        assertNull(SpecificFlightCatalog.store(listOf("unknown")))
        assertEquals("夜間飛行、目視外飛行", SpecificFlightCatalog.format("bvlos,night"))
        assertFalse(SpecificFlightCatalog.isSpecificFlight("none"))
        assertTrue(SpecificFlightCatalog.isSpecificFlight("night,none"))
    }

    @Test
    fun checklistSummarizesKnownResultsOnly() {
        val values = mapOf(
            "airframeResult" to DailyInspectionChecklist.NORMAL,
            "propellerResult" to DailyInspectionChecklist.ABNORMAL,
            "frameResult" to "maybe"
        )
        assertEquals(listOf("プロペラ"), DailyInspectionChecklist.abnormalLabels(values))
        assertEquals(
            "機体全般: 異常なし / プロペラ: 異常あり / 特記: ゆるみ",
            DailyInspectionChecklist.summarize(values, "ゆるみ")
        )
        assertEquals("特記事項なし", DailyInspectionChecklist.summarize(emptyMap(), "  "))
    }

    @Test
    fun snapshotCountsEarlierSameDayFlights() {
        val earlier = flight(id = 1, date = "2026-01-01", minutes = "30分")
        val laterSameDay = flight(id = 3, date = "2026-01-02", minutes = "15分")
        val others = listOf(earlier, laterSameDay)

        assertEquals(
            100,
            FlightHours.snapshotMinutes(60, others, 10, "2026-01-02", thisId = 2)
        )
        assertEquals(
            115,
            FlightHours.snapshotMinutes(60, others, 10, "2026-01-02", thisId = 0)
        )
    }

    @Test
    fun maintenanceStatusUsesIntervalAndFlightsAfterLastWork() {
        val aircraft = Aircraft(
            id = 7,
            registrationSymbol = "JU0001",
            userId = 1,
            initialTotalMinutes = 0,
            maintenanceIntervalHours = 20
        )
        val before = flight(id = 1, date = "2026-01-01", minutes = "10時間", aircraftId = 7)
        val after = flight(id = 2, date = "2026-02-01", minutes = "19時間", aircraftId = 7)
        val maintenance = MaintenanceInspectionRecord(
            id = 1,
            inspectionDate = "2026-01-15",
            location = "格納庫",
            inspectorName = "山田",
            contentAndReason = "定期",
            userId = 1,
            aircraftId = 7
        )

        val withRecord = FlightHours.statuses(listOf(aircraft), listOf(before, after), listOf(maintenance)).single()
        assertEquals("due_soon", withRecord.level)
        assertEquals(19 * 60, withRecord.minutesSinceMaintenance)
        assertEquals(29 * 60, withRecord.totalMinutes)

        val overdue = FlightHours.statuses(
            listOf(aircraft),
            listOf(after.copy(totalFlightTime = "21時間")),
            listOf(maintenance)
        ).single()
        assertEquals("overdue", overdue.level)

        val missing = FlightHours.statuses(listOf(aircraft), listOf(before), emptyList()).single()
        assertEquals("no_record", missing.level)
    }

    @Test
    fun csvEscapesCellsAndStartsWithBom() {
        assertEquals("\"a,b\"", FlightLogCsvExporter.csvCell("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", FlightLogCsvExporter.csvCell("say \"hi\""))
        val csv = FlightLogCsvExporter.export(
            listOf(flight(id = 1, date = "2026-03-01", minutes = "15分", purpose = "調査,点検")),
            mapOf(1 to Aircraft(id = 1, registrationSymbol = "JU1", userId = 1, manufacturer = "Maker"))
        )
        assertTrue(csv.startsWith("\uFEFF登録記号,"))
        assertTrue(csv.contains("\"調査,点検\""))
        assertTrue(csv.contains("Maker"))
    }

    private fun flight(
        id: Int,
        date: String,
        minutes: String,
        aircraftId: Int = 1,
        purpose: String? = null
    ): FlightLog {
        return FlightLog(
            id = id,
            flightDate = date,
            pilotName = "山田",
            userId = 1,
            totalFlightTime = minutes,
            aircraftId = aircraftId,
            flightPurpose = purpose
        )
    }
}
