package utils

import com.opendronediary.model.Aircraft
import com.opendronediary.model.MaintenanceInspectionRecord

object MaintenanceInspectionCsvExporter {
    val headers: List<String> = listOf(
        "点検年月日",
        "場所",
        "実施者",
        "登録記号",
        "実施時点の総飛行時間",
        "点検・修理・改造・整備の内容・理由"
    )

    fun export(records: List<MaintenanceInspectionRecord>, aircraftById: Map<Int, Aircraft>): String {
        val body = records.sortedWith(compareBy({ it.inspectionDate }, { it.id })).joinToString("\n") { record ->
            val aircraft = record.aircraftId?.let { aircraftById[it] }
            listOf(
                record.inspectionDate,
                record.location,
                record.inspectorName,
                aircraft?.registrationSymbol,
                record.totalFlightTime,
                record.contentAndReason
            ).joinToString(",") { FlightLogCsvExporter.csvCell(it) }
        }
        val headerLine = headers.joinToString(",") { FlightLogCsvExporter.csvCell(it) }
        return "\uFEFF$headerLine\n$body\n"
    }
}
