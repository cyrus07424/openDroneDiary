package utils

import com.opendronediary.model.Aircraft
import com.opendronediary.model.FlightLog

/**
 * 様式1（飛行記録）に寄せたCSV。Excelで文字化けしないようBOM付きUTF-8を前提にする。
 */
object FlightLogCsvExporter {
    val headers: List<String> = listOf(
        "登録記号",
        "種類",
        "製造者",
        "型式",
        "製造番号",
        "型式認証書番号",
        "機体認証の区分",
        "機体認証書番号",
        "飛行年月日",
        "飛行させた者の氏名",
        "技能証明番号",
        "飛行の目的",
        "飛行の経路",
        "飛行禁止空域・飛行の方法",
        "離陸場所",
        "離陸時刻",
        "着陸場所",
        "着陸時刻",
        "飛行時間",
        "製造後の総飛行時間",
        "飛行の安全に影響のあった事項",
        "不具合及びその対応",
        "飛行許可・承認番号",
        "飛行概要"
    )

    fun export(logs: List<FlightLog>, aircraftById: Map<Int, Aircraft>): String {
        val body = logs.sortedWith(compareBy({ it.flightDate }, { it.id })).joinToString("\n") { log ->
            val aircraft = log.aircraftId?.let { aircraftById[it] }
            val takeoff = log.takeoffLocation ?: log.takeoffLandingLocation
            val landing = log.landingLocation ?: log.takeoffLandingLocation
            val duration = log.totalFlightTime ?: log.flightDuration
            val cumulative = log.cumulativeFlightMinutes?.let { FlightTimeCalculator.formatMinutes(it) }
            listOf(
                aircraft?.registrationSymbol,
                aircraft?.category,
                aircraft?.manufacturer,
                aircraft?.modelName,
                aircraft?.serialNumber,
                aircraft?.typeCertificateNumber,
                aircraft?.aircraftCertificateClass,
                aircraft?.aircraftCertificateNumber,
                log.flightDate,
                log.pilotName,
                log.skillCertificateNumber,
                log.flightPurpose,
                log.flightRoute,
                SpecificFlightCatalog.format(log.specificFlight).ifBlank { null },
                takeoff,
                log.takeoffTime ?: log.takeoffLandingTime,
                landing,
                log.landingTime,
                duration,
                cumulative,
                log.safetyMatters,
                log.issuesAndResponses,
                log.permissionNumber,
                log.flightSummary
            ).joinToString(",") { csvCell(it) }
        }
        val headerLine = headers.joinToString(",") { csvCell(it) }
        return "\uFEFF$headerLine\n$body\n"
    }

    fun csvCell(value: String?): String {
        val text = value ?: ""
        val escaped = text.replace("\"", "\"\"")
        return if (text.contains(',') || text.contains('"') || text.contains('\n') || text.contains('\r')) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
