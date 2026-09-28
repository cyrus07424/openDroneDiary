package routing

import com.opendronediary.model.Aircraft
import com.opendronediary.model.UserSession
import com.opendronediary.service.AircraftService
import com.opendronediary.service.DailyInspectionRecordService
import com.opendronediary.service.DuplicateRegistrationException
import com.opendronediary.service.FlightLogService
import com.opendronediary.service.MaintenanceInspectionRecordService
import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.div
import kotlinx.html.style
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.hiddenInput
import kotlinx.html.label
import kotlinx.html.numberInput
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.select
import kotlinx.html.small
import kotlinx.html.span
import kotlinx.html.strong
import kotlinx.html.submitInput
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.textArea
import kotlinx.html.textInput
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.unsafe
import utils.DailyInspectionChecklist
import utils.FlightHours
import utils.FlightTimeCalculator
import utils.GTMHelper.addGTMBodyScript
import utils.PolicyHelper.addFooter
import utils.SpecificFlightCatalog

private val aircraftCategories = listOf(
    "回転翼航空機（マルチローター）",
    "回転翼航空機（ヘリコプター）",
    "飛行機",
    "滑空機",
    "飛行船",
    "その他"
)

private val certificateClasses = listOf("第一種", "第二種")

fun Route.configureAircraftRouting(
    aircraftService: AircraftService,
    flightLogService: FlightLogService,
    dailyInspectionRecordService: DailyInspectionRecordService,
    maintenanceInspectionRecordService: MaintenanceInspectionRecordService
) {
    route("/aircraft/ui") {
        get {
            val session = call.sessions.get<UserSession>() ?: run {
                call.respondRedirect("/login")
                return@get
            }
            val aircraft = aircraftService.getAllByUserId(session.userId)
            val logs = flightLogService.getAllByUserId(session.userId)
            val maintenance = maintenanceInspectionRecordService.getAllByUserId(session.userId)
            val statuses = FlightHours.statuses(aircraft, logs, maintenance).associateBy { it.aircraft.id }
            call.respondHtml {
                head { bootstrapHead("機体台帳") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-4") {
                        div(classes = "card") {
                            div(classes = "card-header d-flex justify-content-between align-items-center") {
                                h1(classes = "h4 mb-0") { +"機体台帳" }
                                div {
                                    a(href = "/dashboard", classes = "btn btn-outline-success btn-sm me-2") { +"ダッシュボード" }
                                    a(href = "/", classes = "btn btn-outline-secondary btn-sm") { +"トップ" }
                                }
                            }
                            div(classes = "card-body") {
                                p(classes = "text-muted") {
                                    +"登録記号・型式・製造番号は飛行日誌の冒頭に書く事項です。所有者や使用者が変わったときは、日誌も引き継ぎます。"
                                }
                                if (aircraft.isEmpty()) {
                                    div(classes = "alert alert-info") { +"機体がまだありません。" }
                                } else {
                                    div(classes = "table-responsive") {
                                        table(classes = "table table-striped") {
                                            thead(classes = "table-dark") {
                                                tr {
                                                    th { +"登録記号" }
                                                    th { +"型式" }
                                                    th { +"製造番号" }
                                                    th { +"総飛行時間" }
                                                    th { +"点検" }
                                                    th { +"" }
                                                }
                                            }
                                            tbody {
                                                aircraft.forEach { craft ->
                                                    val status = statuses[craft.id]
                                                    tr {
                                                        td { strong { +craft.registrationSymbol } }
                                                        td { +(listOfNotNull(craft.manufacturer, craft.modelName).joinToString(" ").ifBlank { "—" }) }
                                                        td { +(craft.serialNumber ?: "—") }
                                                        td { +(status?.let { FlightTimeCalculator.formatMinutes(it.totalMinutes) } ?: "—") }
                                                        td {
                                                            val label = when (status?.level) {
                                                                "overdue" -> "期限超過"
                                                                "due_soon" -> "まもなく"
                                                                "no_record" -> "記録なし"
                                                                else -> "良好"
                                                            }
                                                            +label
                                                        }
                                                        td {
                                                            a(href = "/aircraft/ui/${craft.id}/carry", classes = "btn btn-sm btn-outline-primary me-1") { +"携行" }
                                                            a(href = "/aircraft/ui/${craft.id}", classes = "btn btn-sm btn-outline-secondary me-1") { +"編集" }
                                                            form(action = "/aircraft/ui/${craft.id}", method = FormMethod.post, classes = "d-inline") {
                                                                hiddenInput { name = "_method"; value = "delete" }
                                                                submitInput(classes = "btn btn-sm btn-outline-danger") {
                                                                    value = "削除"
                                                                    attributes["onclick"] = "return confirm('機体を削除しますか？関連する記録の機体欄は外れます。')"
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
                        }
                        div(classes = "card mt-4") {
                            div(classes = "card-header") { h2(classes = "h5 mb-0") { +"機体を登録" } }
                            div(classes = "card-body") { aircraftForm(null, "/aircraft/ui") }
                        }
                    }
                    addFooter()
                }
            }
        }

        get("/{id}/carry") {
            val session = call.sessions.get<UserSession>() ?: run {
                call.respondRedirect("/login")
                return@get
            }
            val id = call.parameters["id"]?.toIntOrNull()
            val aircraft = id?.let { aircraftService.getByIdAndUserId(it, session.userId) }
            if (aircraft == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val logs = flightLogService.getAllByUserId(session.userId).filter { it.aircraftId == aircraft.id }
            val inspections = dailyInspectionRecordService.getAllByUserId(session.userId).filter { it.aircraftId == aircraft.id }
            val maintenance = maintenanceInspectionRecordService.getAllByUserId(session.userId).filter { it.aircraftId == aircraft.id }
            val lastMaintenance = maintenance.maxByOrNull { it.inspectionDate }
            val since = lastMaintenance?.inspectionDate
            val carryLogs = logs.filter { since == null || it.flightDate >= since }.sortedBy { it.flightDate }
            val carryInspections = inspections.filter { since == null || it.inspectionDate >= since }.sortedBy { it.inspectionDate }
            call.respondHtml {
                head { bootstrapHead("携行用飛行日誌 ${aircraft.registrationSymbol}") }
                body(classes = "bg-white") {
                    addGTMBodyScript()
                    div(classes = "container my-4") {
                        div(classes = "d-flex justify-content-between no-print") {
                            a(href = "/aircraft/ui", classes = "btn btn-outline-secondary btn-sm") { +"機体台帳へ" }
                            a(href = "javascript:window.print()", classes = "btn btn-primary btn-sm") { +"印刷 / PDF保存" }
                        }
                        h1(classes = "h3 mt-3") { +"無人航空機 飛行日誌（携行用）" }
                        p { +"登録記号: ${aircraft.registrationSymbol}" }
                        p {
                            +"種類: ${aircraft.category ?: "—"} / 製造者: ${aircraft.manufacturer ?: "—"} / 型式: ${aircraft.modelName ?: "—"} / 製造番号: ${aircraft.serialNumber ?: "—"}"
                        }
                        p {
                            +"型式認証書番号: ${aircraft.typeCertificateNumber ?: "—"} / 機体認証: ${aircraft.aircraftCertificateClass ?: "—"} ${aircraft.aircraftCertificateNumber ?: ""}"
                        }
                        p(classes = "small text-muted") {
                            +"直近の点検整備以降の記録を表示しています。機体を選んでいない記録は含まれません。"
                        }
                        h2(classes = "h5") { +"点検整備記録" }
                        if (lastMaintenance == null) {
                            p { +"点検整備記録はまだありません。" }
                        } else {
                            p {
                                +"${lastMaintenance.inspectionDate} ${lastMaintenance.location} / ${lastMaintenance.inspectorName} / 総飛行時間 ${lastMaintenance.totalFlightTime ?: "—"}"
                            }
                            p { +lastMaintenance.contentAndReason }
                        }
                        h2(classes = "h5 mt-4") { +"日常点検記録" }
                        if (carryInspections.isEmpty()) {
                            p { +"対象期間の日常点検記録はありません。" }
                        } else {
                            table(classes = "table table-sm table-bordered") {
                                thead {
                                    tr {
                                        th { +"日付" }
                                        th { +"場所" }
                                        th { +"実施者" }
                                        th { +"結果" }
                                    }
                                }
                                tbody {
                                    carryInspections.forEach { record ->
                                        tr {
                                            td { +record.inspectionDate }
                                            td { +record.location }
                                            td { +record.inspectorName }
                                            td {
                                                val abnormal = DailyInspectionChecklist.abnormalLabels(record.checklistValues())
                                                if (abnormal.isEmpty()) {
                                                    +"異常なし"
                                                } else {
                                                    +"異常: ${abnormal.joinToString("、")}"
                                                }
                                                if (record.inspectionResult.isNotBlank()) {
                                                    br()
                                                    small { +record.inspectionResult }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        h2(classes = "h5 mt-4") { +"飛行記録" }
                        if (carryLogs.isEmpty()) {
                            p { +"対象期間の飛行記録はありません。" }
                        } else {
                            carryLogs.forEach { log ->
                                div(classes = "border rounded p-2 mb-2") {
                                    h3(classes = "h6") { +"${log.flightDate} ${log.pilotName}" }
                                    p(classes = "mb-1") {
                                        +"技能証明: ${log.skillCertificateNumber ?: "—"} / 目的: ${log.flightPurpose ?: "—"} / 経路: ${log.flightRoute ?: "—"}"
                                    }
                                    p(classes = "mb-1") {
                                        +"離陸 ${log.takeoffTime ?: "—"} ${log.takeoffLocation ?: log.takeoffLandingLocation ?: ""} / 着陸 ${log.landingTime ?: "—"} ${log.landingLocation ?: ""}"
                                    }
                                    p(classes = "mb-1") {
                                        +"飛行時間 ${log.totalFlightTime ?: log.flightDuration ?: "—"} / 製造後 ${log.cumulativeFlightMinutes?.let { FlightTimeCalculator.formatMinutes(it) } ?: "—"}"
                                    }
                                    p(classes = "mb-1") { +"空域・方法: ${SpecificFlightCatalog.format(log.specificFlight).ifBlank { "—" }}" }
                                    p(classes = "mb-0") {
                                        +"安全に影響のあった事項: ${log.safetyMatters ?: "—"} / 不具合: ${log.issuesAndResponses ?: "—"}"
                                    }
                                }
                            }
                        }
                    }
                    style {
                    unsafe { +"@media print { .no-print { display: none !important; } }" }
                }
                }
            }
        }

        get("/{id}") {
            val session = call.sessions.get<UserSession>() ?: run {
                call.respondRedirect("/login")
                return@get
            }
            val id = call.parameters["id"]?.toIntOrNull()
            val aircraft = id?.let { aircraftService.getByIdAndUserId(it, session.userId) }
            if (aircraft == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respondHtml {
                head { bootstrapHead("機体編集") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-4") {
                        div(classes = "card") {
                            div(classes = "card-header") { h1(classes = "h4 mb-0") { +"機体を編集" } }
                            div(classes = "card-body") { aircraftForm(aircraft, "/aircraft/ui/${aircraft.id}", updating = true) }
                        }
                    }
                    addFooter()
                }
            }
        }

        post("/{id}") {
            val session = call.sessions.get<UserSession>() ?: run {
                call.respondRedirect("/login")
                return@post
            }
            val id = call.parameters["id"]?.toIntOrNull()
            val params = call.receiveParameters()
            when (params["_method"]) {
                "delete" -> {
                    if (id != null && aircraftService.delete(id, session.userId)) {
                        call.respondRedirect("/aircraft/ui")
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                "put" -> {
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    try {
                        val updated = aircraftService.update(id, params.toAircraft(session.userId), session.userId)
                        if (updated) call.respondRedirect("/aircraft/ui") else call.respond(HttpStatusCode.NotFound)
                    } catch (e: DuplicateRegistrationException) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "登録記号が重複しています")
                    }
                }
                else -> call.respond(HttpStatusCode.BadRequest)
            }
        }

        post {
            val session = call.sessions.get<UserSession>() ?: run {
                call.respondRedirect("/login")
                return@post
            }
            val params = call.receiveParameters()
                        val symbol = params["registrationSymbol"].blankToNull()
                        if (symbol == null) {
                            call.respond(HttpStatusCode.BadRequest, "登録記号は必須です")
                            return@post
                        }
                        try {
                            aircraftService.add(params.toAircraft(session.userId))
                            call.respondRedirect("/aircraft/ui")
            } catch (e: DuplicateRegistrationException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "登録記号が重複しています")
            }
        }
    }
}

private fun io.ktor.http.Parameters.toAircraft(userId: Int): Aircraft {
    val hours = this["initialHours"]?.toIntOrNull() ?: 0
    val minutes = this["initialMinutes"]?.toIntOrNull() ?: 0
    return Aircraft(
        id = 0,
        registrationSymbol = this["registrationSymbol"] ?: "",
        userId = userId,
        manufacturer = this["manufacturer"].blankToNull(),
        modelName = this["modelName"].blankToNull(),
        serialNumber = this["serialNumber"].blankToNull(),
        category = this["category"].blankToNull(),
        typeCertificateNumber = this["typeCertificateNumber"].blankToNull(),
        aircraftCertificateClass = this["aircraftCertificateClass"].blankToNull(),
        aircraftCertificateNumber = this["aircraftCertificateNumber"].blankToNull(),
        initialTotalMinutes = hours.coerceAtLeast(0) * 60 + minutes.coerceIn(0, 59),
        maintenanceIntervalHours = this["maintenanceIntervalHours"]?.toIntOrNull() ?: 20,
        notes = this["notes"].blankToNull()
    )
}

private fun kotlinx.html.FlowContent.aircraftForm(aircraft: Aircraft?, action: String, updating: Boolean = false) {
    val initialHours = (aircraft?.initialTotalMinutes ?: 0) / 60
    val initialMinutes = (aircraft?.initialTotalMinutes ?: 0) % 60
    form(action = action, method = FormMethod.post) {
        if (updating) hiddenInput { name = "_method"; value = "put" }
        div(classes = "row") {
            div(classes = "col-md-4 mb-3") {
                label(classes = "form-label") { +"登録記号" }
                textInput(classes = "form-control") {
                    name = "registrationSymbol"
                    required = true
                    value = aircraft?.registrationSymbol ?: ""
                    placeholder = "例: JU1234567890"
                    maxLength = "32"
                }
            }
            div(classes = "col-md-4 mb-3") {
                label(classes = "form-label") { +"種類" }
                select(classes = "form-select") {
                    name = "category"
                    option { value = ""; +"未選択" }
                    aircraftCategories.forEach { category ->
                        option {
                            value = category
                            if (aircraft?.category == category) selected = true
                            +category
                        }
                    }
                }
            }
            div(classes = "col-md-4 mb-3") {
                label(classes = "form-label") { +"製造者" }
                textInput(classes = "form-control") {
                    name = "manufacturer"
                    value = aircraft?.manufacturer ?: ""
                }
            }
        }
        div(classes = "row") {
            div(classes = "col-md-4 mb-3") {
                label(classes = "form-label") { +"型式" }
                textInput(classes = "form-control") {
                    name = "modelName"
                    value = aircraft?.modelName ?: ""
                }
            }
            div(classes = "col-md-4 mb-3") {
                label(classes = "form-label") { +"製造番号" }
                textInput(classes = "form-control") {
                    name = "serialNumber"
                    value = aircraft?.serialNumber ?: ""
                }
            }
            div(classes = "col-md-4 mb-3") {
                label(classes = "form-label") { +"型式認証書番号" }
                textInput(classes = "form-control") {
                    name = "typeCertificateNumber"
                    value = aircraft?.typeCertificateNumber ?: ""
                }
            }
        }
        div(classes = "row") {
            div(classes = "col-md-4 mb-3") {
                label(classes = "form-label") { +"機体認証の区分" }
                select(classes = "form-select") {
                    name = "aircraftCertificateClass"
                    option { value = ""; +"なし / 未取得" }
                    certificateClasses.forEach { clazz ->
                        option {
                            value = clazz
                            if (aircraft?.aircraftCertificateClass == clazz) selected = true
                            +clazz
                        }
                    }
                }
            }
            div(classes = "col-md-4 mb-3") {
                label(classes = "form-label") { +"機体認証書番号" }
                textInput(classes = "form-control") {
                    name = "aircraftCertificateNumber"
                    value = aircraft?.aircraftCertificateNumber ?: ""
                }
            }
            div(classes = "col-md-4 mb-3") {
                label(classes = "form-label") { +"定期点検の目安（時間）" }
                numberInput(classes = "form-control") {
                    name = "maintenanceIntervalHours"
                    value = (aircraft?.maintenanceIntervalHours ?: 20).toString()
                    min = "1"
                    max = "500"
                }
                small(classes = "text-muted") { +"メーカー指定がなければ20" }
            }
        }
        div(classes = "row") {
            div(classes = "col-md-3 mb-3") {
                label(classes = "form-label") { +"引き継ぎ済みの飛行時間" }
                numberInput(classes = "form-control") {
                    name = "initialHours"
                    value = initialHours.toString()
                    min = "0"
                }
            }
            div(classes = "col-md-3 mb-3") {
                label(classes = "form-label") { +"分" }
                numberInput(classes = "form-control") {
                    name = "initialMinutes"
                    value = initialMinutes.toString()
                    min = "0"
                    max = "59"
                }
            }
            div(classes = "col-md-6 mb-3") {
                label(classes = "form-label") { +"メモ" }
                textArea(classes = "form-control") {
                    name = "notes"
                    rows = "2"
                    +(aircraft?.notes ?: "")
                }
            }
        }
        submitInput(classes = "btn btn-success") { value = if (updating) "更新" else "機体を登録" }
    }
}
