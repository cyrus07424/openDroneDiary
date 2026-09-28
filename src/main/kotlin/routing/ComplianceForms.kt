package routing

import com.opendronediary.model.Aircraft
import com.opendronediary.model.DailyInspectionRecord
import com.opendronediary.model.FlightLog
import com.opendronediary.service.PilotService
import io.ktor.http.Parameters
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.br
import kotlinx.html.button
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.h5
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.radioInput
import kotlinx.html.select
import kotlinx.html.small
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.textArea
import kotlinx.html.textInput
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import utils.DailyInspectionChecklist
import utils.SpecificFlightCatalog

fun String?.blankToNull(): String? = this?.trim()?.ifEmpty { null }

fun FlightLog.bindFlightCompliance(params: Parameters, pilotService: PilotService, userId: Int): FlightLog {
    val fromForm = params["skillCertificateNumber"].blankToNull()
    val resolvedSkill = fromForm ?: pilotId?.let { pilotService.getByIdAndUserId(it, userId)?.skillCertificateNumber }
    return copy(
        aircraftId = params["aircraftId"]?.toIntOrNull(),
        flightPurpose = params["flightPurpose"].blankToNull(),
        flightRoute = params["flightRoute"].blankToNull(),
        specificFlight = SpecificFlightCatalog.store(params.getAll("specificFlight") ?: emptyList()),
        safetyMatters = params["safetyMatters"].blankToNull(),
        skillCertificateNumber = resolvedSkill.blankToNull(),
        permissionNumber = params["permissionNumber"].blankToNull()
    )
}

fun Parameters.bindDailyInspection(id: Int, userId: Int): DailyInspectionRecord {
    val values = DailyInspectionChecklist.items.associate { item ->
        val raw = this[item.field].blankToNull()
        item.field to raw?.takeIf { DailyInspectionChecklist.isKnown(it) }
    }
    return DailyInspectionRecord(
        id = id,
        inspectionDate = this["inspectionDate"] ?: "",
        location = this["location"] ?: "",
        inspectorName = this["inspectorName"] ?: "",
        inspectionResult = this["inspectionResult"].blankToNull() ?: "",
        userId = userId,
        aircraftId = this["aircraftId"]?.toIntOrNull(),
        airframeResult = values["airframeResult"],
        propellerResult = values["propellerResult"],
        frameResult = values["frameResult"],
        communicationResult = values["communicationResult"],
        propulsionResult = values["propulsionResult"],
        powerResult = values["powerResult"],
        automaticControlResult = values["automaticControlResult"],
        controllerResult = values["controllerResult"],
        batteryResult = values["batteryResult"]
    )
}

fun FlowContent.aircraftSelectField(aircraft: List<Aircraft>, selectedId: Int?) {
    div(classes = "mb-3") {
        label(classes = "form-label") { +"機体（登録記号）" }
        select(classes = "form-select") {
            name = "aircraftId"
            option {
                value = ""
                +"選択しない"
            }
            aircraft.forEach { craft ->
                option {
                    value = craft.id.toString()
                    if (craft.id == selectedId) selected = true
                    +craft.displayName()
                }
            }
        }
        p(classes = "form-text mb-0") {
            +"飛行日誌は機体ごとに残します。"
            a(href = "/aircraft/ui", classes = "ms-1") { +"機体台帳" }
        }
    }
}

fun FlowContent.flightComplianceFields(aircraft: List<Aircraft>, log: FlightLog?) {
    val selectedCodes = SpecificFlightCatalog.parse(log?.specificFlight)
    div(classes = "card border-success mb-4") {
        div(classes = "card-header bg-success text-white") {
            h5(classes = "mb-0") { +"飛行日誌の記載事項" }
        }
        div(classes = "card-body") {
            p(classes = "text-muted small") {
                +"航空法の飛行記録（様式1）に合わせた項目です。特定飛行では記載と携行が必要です。"
            }
            aircraftSelectField(aircraft, log?.aircraftId)
            div(classes = "row") {
                div(classes = "col-md-6 mb-3") {
                    label(classes = "form-label") { +"飛行の目的" }
                    textInput(classes = "form-control") {
                        name = "flightPurpose"
                        value = log?.flightPurpose ?: ""
                        placeholder = "例: 空撮、測量、訓練、農薬散布"
                    }
                }
                div(classes = "col-md-6 mb-3") {
                    label(classes = "form-label") { +"技能証明番号" }
                    textInput(classes = "form-control") {
                        name = "skillCertificateNumber"
                        value = log?.skillCertificateNumber ?: ""
                        placeholder = "未入力なら登録パイロットの番号を転記"
                    }
                }
            }
            div(classes = "mb-3") {
                label(classes = "form-label") { +"飛行の経路" }
                textArea(classes = "form-control") {
                    name = "flightRoute"
                    rows = "2"
                    placeholder = "例: 離着陸地点の周辺、東西に往復"
                    +(log?.flightRoute ?: "")
                }
            }
            div(classes = "mb-3") {
                label(classes = "form-label") { +"飛行禁止空域・飛行の方法" }
                SpecificFlightCatalog.all.groupBy { it.group }.forEach { (group, categories) ->
                    small(classes = "text-muted d-block mt-2") { +group }
                    div(classes = "row") {
                        categories.forEach { category ->
                            div(classes = "col-md-6") {
                                div(classes = "form-check") {
                                    val boxId = "specific-${category.code}"
                                    checkBoxInput(classes = "form-check-input") {
                                        name = "specificFlight"
                                        value = category.code
                                        id = boxId
                                        checked = category.code in selectedCodes
                                    }
                                    label(classes = "form-check-label") {
                                        htmlFor = boxId
                                        +category.label
                                    }
                                }
                            }
                        }
                    }
                }
            }
            div(classes = "row") {
                div(classes = "col-md-6 mb-3") {
                    label(classes = "form-label") { +"飛行の安全に影響のあった事項" }
                    textArea(classes = "form-control") {
                        name = "safetyMatters"
                        rows = "2"
                        placeholder = "なければ「なし」"
                        +(log?.safetyMatters ?: "")
                    }
                }
                div(classes = "col-md-6 mb-3") {
                    label(classes = "form-label") { +"飛行許可・承認番号（任意）" }
                    textInput(classes = "form-control") {
                        name = "permissionNumber"
                        value = log?.permissionNumber ?: ""
                        placeholder = "国交省の許可・承認を受けた飛行の場合"
                    }
                }
            }
            p(classes = "form-text mb-0") {
                +"製造後の総飛行時間は、機体を選ぶと保存時に自動計算されます。"
            }
        }
    }
}

fun FlowContent.dailyChecklistFields(record: DailyInspectionRecord?) {
    val values = record?.checklistValues() ?: emptyMap()
    div(classes = "card border-secondary mb-3") {
        div(classes = "card-header d-flex justify-content-between align-items-center") {
            h5(classes = "mb-0") { +"日常点検項目（様式2）" }
            button(classes = "btn btn-sm btn-outline-success", type = kotlinx.html.ButtonType.button) {
                attributes["onclick"] = "document.querySelectorAll('.check-normal').forEach(function(el){ el.checked = true; })"
                +"すべて異常なし"
            }
        }
        div(classes = "card-body p-0") {
            div(classes = "table-responsive") {
                table(classes = "table table-sm mb-0") {
                    thead {
                        tr {
                            th { +"点検項目" }
                            th { +"結果" }
                        }
                    }
                    tbody {
                        DailyInspectionChecklist.items.forEach { item ->
                            tr {
                                td {
                                    +item.label
                                    br()
                                    small(classes = "text-muted") { +item.hint }
                                }
                                td {
                                    div(classes = "form-check form-check-inline") {
                                        val normalId = "${item.field}-normal"
                                        radioInput(classes = "form-check-input check-normal", name = item.field) {
                                            id = normalId
                                            value = DailyInspectionChecklist.NORMAL
                                            checked = values[item.field] == DailyInspectionChecklist.NORMAL
                                        }
                                        label(classes = "form-check-label") {
                                            htmlFor = normalId
                                            +"異常なし"
                                        }
                                    }
                                    div(classes = "form-check form-check-inline") {
                                        val abnormalId = "${item.field}-abnormal"
                                        radioInput(classes = "form-check-input", name = item.field) {
                                            id = abnormalId
                                            value = DailyInspectionChecklist.ABNORMAL
                                            checked = values[item.field] == DailyInspectionChecklist.ABNORMAL
                                        }
                                        label(classes = "form-check-label") {
                                            htmlFor = abnormalId
                                            +"異常あり"
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
