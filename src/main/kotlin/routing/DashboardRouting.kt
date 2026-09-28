package routing

import com.opendronediary.model.UserSession
import com.opendronediary.service.AircraftService
import com.opendronediary.service.DailyInspectionRecordService
import com.opendronediary.service.FlightLogService
import com.opendronediary.service.MaintenanceInspectionRecordService
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.h5
import kotlinx.html.p
import kotlinx.html.small
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import utils.DailyInspectionChecklist
import utils.FlightHours
import utils.FlightTimeCalculator
import utils.GTMHelper.addGTMBodyScript
import utils.SpecificFlightCatalog
import java.time.LocalDate

fun Route.configureDashboardRouting(
    aircraftService: AircraftService,
    flightLogService: FlightLogService,
    dailyInspectionRecordService: DailyInspectionRecordService,
    maintenanceInspectionRecordService: MaintenanceInspectionRecordService
) {
    get("/dashboard") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }
        val aircraft = aircraftService.getAllByUserId(session.userId)
        val logs = flightLogService.getAllByUserId(session.userId)
        val maintenance = maintenanceInspectionRecordService.getAllByUserId(session.userId)
        val inspections = dailyInspectionRecordService.getAllByUserId(session.userId)
        val statuses = FlightHours.statuses(aircraft, logs, maintenance)
        val monthPrefix = LocalDate.now().toString().substring(0, 7)
        val monthLogs = logs.filter { it.flightDate.startsWith(monthPrefix) }
        val monthMinutes = monthLogs.sumOf { FlightHours.flightMinutes(it) }
        val specificCount = logs.count { SpecificFlightCatalog.isSpecificFlight(it.specificFlight) }
        val abnormalCount = inspections.count { DailyInspectionChecklist.abnormalLabels(it.checklistValues()).isNotEmpty() }
        val aircraftById = aircraft.associateBy { it.id }

        call.respondHtml {
            head { bootstrapHead("ダッシュボード") }
            body(classes = "d-flex flex-column min-vh-100") {
                addGTMBodyScript()
                div(classes = "container mt-4 mb-5") {
                    div(classes = "d-flex justify-content-between align-items-center mb-3") {
                        h1(classes = "h3 mb-0") { +"飛行日誌ダッシュボード" }
                        div {
                            a(href = "/", classes = "btn btn-outline-secondary btn-sm me-2") { +"トップ" }
                            a(href = "/flightlogs/export.csv", classes = "btn btn-outline-success btn-sm") { +"飛行記録CSV" }
                        }
                    }
                    div(classes = "alert alert-light border") {
                        +"特定飛行では、飛行記録・日常点検記録・点検整備記録を機体ごとに記載し、飛行時に携行します（航空法第132条の89）。電子データでもかまいません。"
                    }
                    div(classes = "row g-3 mb-4") {
                        summaryCard("機体", aircraft.size.toString(), "台帳")
                        summaryCard("今月の飛行", monthLogs.size.toString() + "回", FlightTimeCalculator.formatMinutes(monthMinutes))
                        summaryCard("特定飛行の記録", specificCount.toString(), "件")
                        summaryCard("異常ありの日常点検", abnormalCount.toString(), "件")
                    }
                    div(classes = "card mb-4") {
                        div(classes = "card-header") {
                            h2(classes = "h5 mb-0") { +"点検の目安" }
                        }
                        div(classes = "card-body") {
                            if (statuses.isEmpty()) {
                                p { +"機体が未登録です。" }
                                a(href = "/aircraft/ui", classes = "btn btn-primary btn-sm") { +"機体を登録" }
                            } else {
                                p(classes = "text-muted small") {
                                    +"メーカー指定がなければ、点検整備は20時間ごとが目安です。期限が近い機体から確認してください。"
                                }
                                div(classes = "table-responsive") {
                                    table(classes = "table table-sm align-middle") {
                                        thead {
                                            tr {
                                                th { +"登録記号" }
                                                th { +"製造後の総飛行時間" }
                                                th { +"点検後" }
                                                th { +"状態" }
                                                th { +"" }
                                            }
                                        }
                                        tbody {
                                            statuses.sortedBy { it.remainingMinutes }.forEach { status ->
                                                tr {
                                                    td { +status.aircraft.registrationSymbol }
                                                    td { +FlightTimeCalculator.formatMinutes(status.totalMinutes) }
                                                    td { +FlightTimeCalculator.formatMinutes(status.minutesSinceMaintenance) }
                                                    td {
                                                        val (css, text) = when (status.level) {
                                                            "overdue" -> "badge bg-danger" to "点検時期を過ぎています"
                                                            "due_soon" -> "badge bg-warning text-dark" to "まもなく点検時期"
                                                            "no_record" -> "badge bg-secondary" to "点検記録なし"
                                                            else -> "badge bg-success" to "余裕あり"
                                                        }
                                                        span(classes = css) { +text }
                                                        status.lastMaintenanceDate?.let {
                                                            br()
                                                            small(classes = "text-muted") { +"前回: $it" }
                                                        }
                                                    }
                                                    td {
                                                        a(href = "/aircraft/ui/${status.aircraft.id}/carry", classes = "btn btn-sm btn-outline-primary") {
                                                            +"携行用"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    div(classes = "card") {
                        div(classes = "card-header") {
                            h2(classes = "h5 mb-0") { +"最近の飛行" }
                        }
                        div(classes = "card-body") {
                            if (logs.isEmpty()) {
                                p { +"飛行記録はまだありません。" }
                            } else {
                                div(classes = "table-responsive") {
                                    table(classes = "table table-sm") {
                                        thead {
                                            tr {
                                                th { +"日付" }
                                                th { +"機体" }
                                                th { +"操縦者" }
                                                th { +"飛行時間" }
                                                th { +"区分" }
                                            }
                                        }
                                        tbody {
                                            logs.sortedByDescending { it.flightDate }.take(8).forEach { log ->
                                                tr {
                                                    td { +log.flightDate }
                                                    td { +(log.aircraftId?.let { aircraftById[it]?.registrationSymbol } ?: "未設定") }
                                                    td { +log.pilotName }
                                                    td { +(log.totalFlightTime ?: log.flightDuration ?: "—") }
                                                    td { +(SpecificFlightCatalog.format(log.specificFlight).ifBlank { "—" }) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            a(href = "/flightlogs/ui", classes = "btn btn-primary btn-sm") { +"飛行記録へ" }
                        }
                    }
                }
            }
        }
    }
}

private fun kotlinx.html.DIV.summaryCard(title: String, value: String, detail: String) {
    div(classes = "col-md-3") {
        div(classes = "card h-100") {
            div(classes = "card-body") {
                h5(classes = "card-title text-muted") { +title }
                p(classes = "display-6 mb-0") { +value }
                small(classes = "text-muted") { +detail }
            }
        }
    }
}
