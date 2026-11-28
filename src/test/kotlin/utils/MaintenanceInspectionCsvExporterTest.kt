package utils

import com.opendronediary.model.Aircraft
import com.opendronediary.model.MaintenanceInspectionRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class MaintenanceInspectionCsvExporterTest {
    @Test
    fun exportsHeaderAndBomEvenWithoutRecords() {
        assertEquals(
            "\uFEFF点検年月日,場所,実施者,登録記号,実施時点の総飛行時間,点検・修理・改造・整備の内容・理由\n\n",
            MaintenanceInspectionCsvExporter.export(emptyList(), emptyMap())
        )
    }

    @Test
    fun sortsRowsAndEscapesCellsWithMissingAircraft() {
        val records = listOf(
            record(3, "2026-02-01", "later", aircraftId = 99),
            record(2, "2026-01-01", "second"),
            record(1, "2026-01-01", "first, \"quoted\"\nnext", aircraftId = 7, totalFlightTime = "10時間")
        )
        val aircraft = Aircraft(id = 7, registrationSymbol = "JU,1", userId = 1)

        assertEquals(
            "\uFEFF点検年月日,場所,実施者,登録記号,実施時点の総飛行時間,点検・修理・改造・整備の内容・理由\n" +
                "2026-01-01,\"first, \"\"quoted\"\"\nnext\",inspector,\"JU,1\",10時間,work\n" +
                "2026-01-01,second,inspector,,,work\n" +
                "2026-02-01,later,inspector,,,work\n",
            MaintenanceInspectionCsvExporter.export(records, mapOf(7 to aircraft))
        )
    }

    private fun record(
        id: Int,
        date: String,
        location: String,
        aircraftId: Int? = null,
        totalFlightTime: String? = null
    ) = MaintenanceInspectionRecord(
        id = id,
        inspectionDate = date,
        location = location,
        inspectorName = "inspector",
        contentAndReason = "work",
        userId = 1,
        aircraftId = aircraftId,
        totalFlightTime = totalFlightTime
    )
}
