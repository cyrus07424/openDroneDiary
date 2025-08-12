package routing

import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.http.*
import com.opendronediary.model.FlightLog
import com.opendronediary.model.UserSession
import com.opendronediary.service.FlightLogService
import com.opendronediary.service.PilotService
import com.opendronediary.service.SlackService
import io.ktor.server.html.respondHtml
import kotlinx.html.*
import utils.GTMHelper.addGTMBodyScript
import utils.PolicyHelper.addFooter
import utils.RequestContextHelper
import routing.bootstrapHead
import java.math.BigDecimal

fun Route.configureFlightLogRouting(flightLogService: FlightLogService, slackService: SlackService, pilotService: PilotService) {
    // 飛行記録 CRUD - Authentication required
    route("/flightlogs") {
        get {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            call.respond(flightLogService.getAllByUserId(session.userId))
        }
        get("/{id}") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            val id = call.parameters["id"]?.toIntOrNull()
            val flightLog = id?.let { flightLogService.getByIdAndUserId(it, session.userId) }
            if (flightLog != null) {
                call.respond(flightLog)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        post {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val contentType = call.request.contentType()
            if (contentType.match(ContentType.Application.FormUrlEncoded)) {
                val params = call.receiveParameters()
                val flightDate = params["flightDate"] ?: ""
                val pilotName = params["pilotName"] ?: ""
                val issuesAndResponses = params["issuesAndResponses"]
                
                // Handle new fields
                val takeoffLocation = params["takeoffLocation"]
                val landingLocation = params["landingLocation"]
                val takeoffTime = params["takeoffTime"]
                val landingTime = params["landingTime"]
                val flightSummary = params["flightSummary"]
                val totalFlightTime = params["totalFlightTime"]
                
                // Legacy fields for backward compatibility
                val takeoffLandingLocation = params["takeoffLandingLocation"]
                val takeoffLandingTime = params["takeoffLandingTime"]
                val flightDuration = params["flightDuration"]
                
                // Handle coordinate fields
                val takeoffInputType = params["takeoffInputType"] ?: "text"
                val landingInputType = params["landingInputType"] ?: "text"
                val takeoffLatitude = params["takeoffLatitude"]?.toBigDecimalOrNull()
                val takeoffLongitude = params["takeoffLongitude"]?.toBigDecimalOrNull()
                val landingLatitude = params["landingLatitude"]?.toBigDecimalOrNull()
                val landingLongitude = params["landingLongitude"]?.toBigDecimalOrNull()
                
                val created = flightLogService.add(FlightLog(
                    id = 0, 
                    flightDate = flightDate, 
                    takeoffLandingLocation = takeoffLandingLocation, 
                    takeoffLandingTime = takeoffLandingTime, 
                    flightDuration = flightDuration, 
                    pilotName = pilotName, 
                    issuesAndResponses = issuesAndResponses, 
                    userId = session.userId,
                    takeoffLocation = takeoffLocation,
                    landingLocation = landingLocation,
                    takeoffTime = takeoffTime,
                    landingTime = landingTime,
                    flightSummary = flightSummary,
                    totalFlightTime = totalFlightTime,
                    takeoffInputType = takeoffInputType,
                    landingInputType = landingInputType,
                    takeoffLatitude = takeoffLatitude,
                    takeoffLongitude = takeoffLongitude,
                    landingLatitude = landingLatitude,
                    landingLongitude = landingLongitude
                ))
                
                // Send Slack notification for flight log creation
                try {
                    val userAgent = RequestContextHelper.extractUserAgent(call)
                    val ipAddress = RequestContextHelper.extractIpAddress(call)
                    slackService.sendNotification(
                        action = "飛行記録作成",
                        username = session.username,
                        userAgent = userAgent,
                        ipAddress = ipAddress,
                        additionalInfo = "飛行日: $flightDate, パイロット: $pilotName"
                    )
                } catch (e: Exception) {
                    println("Error: " + "Failed to send Slack notification for flight log creation" + ": " + e.message)
                }
                
            } else {
                val flightLog = call.receive<FlightLog>()
                val created = flightLogService.add(flightLog.copy(userId = session.userId))
                
                // Send Slack notification for API flight log creation
                try {
                    val userAgent = RequestContextHelper.extractUserAgent(call)
                    val ipAddress = RequestContextHelper.extractIpAddress(call)
                    slackService.sendNotification(
                        action = "飛行記録作成 (API)",
                        username = session.username,
                        userAgent = userAgent,
                        ipAddress = ipAddress,
                        additionalInfo = "飛行日: ${flightLog.flightDate}, パイロット: ${flightLog.pilotName}"
                    )
                } catch (e: Exception) {
                    println("Error: " + "Failed to send Slack notification for API flight log creation" + ": " + e.message)
                }
                
                call.respond(HttpStatusCode.Created, created)
            }
        }
        put("/{id}") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }
            val id = call.parameters["id"]?.toIntOrNull()
            val flightLog = call.receive<FlightLog>()
            if (id != null && flightLogService.update(id, flightLog, session.userId)) {
                // Send Slack notification for flight log update
                try {
                    val userAgent = RequestContextHelper.extractUserAgent(call)
                    val ipAddress = RequestContextHelper.extractIpAddress(call)
                    slackService.sendNotification(
                        action = "飛行記録更新",
                        username = session.username,
                        userAgent = userAgent,
                        ipAddress = ipAddress,
                        additionalInfo = "ID: $id, 飛行日: ${flightLog.flightDate}"
                    )
                } catch (e: Exception) {
                    println("Error: " + "Failed to send Slack notification for flight log update" + ": " + e.message)
                }
                
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        delete("/{id}") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@delete
            }
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null && flightLogService.delete(id, session.userId)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    // 飛行記録 UI (HTML画面) - Authentication required
    route("/flightlogs/ui") {
        get {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respondRedirect("/login")
                return@get
            }
            val flightLogs = flightLogService.getAllByUserId(session.userId)
            call.respondHtml {
                head { 
                    bootstrapHead("飛行記録一覧")
                    // Add Leaflet CSS and JS for map functionality
                    link(rel = "stylesheet", href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css")
                    script(src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js") {}
                }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    nav(classes = "navbar navbar-expand-lg navbar-dark bg-dark") {
                        div(classes = "container") {
                            a(href = "/", classes = "navbar-brand") { +"🛩️ OpenDroneDiary" }
                            div(classes = "navbar-nav ms-auto") {
                                span(classes = "navbar-text me-3") { +"ログイン中: ${session.username}" }
                                a(href = "/logout", classes = "btn btn-outline-light btn-sm") { +"ログアウト" }
                            }
                        }
                    }
                    div(classes = "container mt-4") {
                        div(classes = "row") {
                            div(classes = "col-12") {
                                div(classes = "card") {
                                    div(classes = "card-header d-flex justify-content-between align-items-center") {
                                        h1(classes = "card-title mb-0") { +"飛行記録一覧" }
                                        div {
                                            a(href = "/flightlogs/ui/heatmap", classes = "btn btn-outline-warning btn-sm me-2") { +"🗺️ ヒートマップ" }
                                            a(href = "/flightlogs/ui/calendar", classes = "btn btn-outline-info btn-sm me-2") { +"📅 カレンダー表示" }
                                            a(href = "/flightlogs/ui/timeline", classes = "btn btn-outline-info btn-sm me-2") { +"📊 タイムライン表示" }
                                            a(href = "/", classes = "btn btn-outline-primary btn-sm") { +"トップへ" }
                                        }
                                    }
                                    div(classes = "card-body") {
                                        if (flightLogs.isEmpty()) {
                                            div(classes = "alert alert-info") { +"まだ飛行記録がありません。下のフォームから新規作成してください。" }
                                        } else {
                                            div(classes = "table-responsive") {
                                                table(classes = "table table-striped table-hover") {
                                                    thead(classes = "table-dark") {
                                                        tr {
                                                            th { +"ID" }
                                                            th { +"飛行日" }
                                                            th { +"離着陸場所" }
                                                            th { +"時刻" }
                                                            th { +"飛行時間" }
                                                            th { +"操縦者" }
                                                            th(classes = "text-center") { +"操作" }
                                                        }
                                                    }
                                                    tbody {
                                                        flightLogs.forEach { flightLog ->
                                                            tr {
                                                                td { +flightLog.id.toString() }
                                                                td { +flightLog.flightDate }
                                                                td { 
                                                                    // Use new fields if available, fallback to legacy field
                                                                    if (!flightLog.takeoffLocation.isNullOrEmpty() && !flightLog.landingLocation.isNullOrEmpty()) {
                                                                        +"${flightLog.takeoffLocation} → ${flightLog.landingLocation}"
                                                                        if (flightLog.takeoffInputType == "coordinates" && 
                                                                            flightLog.takeoffLatitude != null && flightLog.takeoffLongitude != null &&
                                                                            flightLog.landingLatitude != null && flightLog.landingLongitude != null) {
                                                                            br()
                                                                            small(classes = "text-muted") { 
                                                                                +"📍 座標: (${flightLog.takeoffLatitude?.toPlainString()?.take(8)}, ${flightLog.takeoffLongitude?.toPlainString()?.take(8)}) → (${flightLog.landingLatitude?.toPlainString()?.take(8)}, ${flightLog.landingLongitude?.toPlainString()?.take(8)})"
                                                                            }
                                                                        }
                                                                    } else if (flightLog.takeoffInputType == "coordinates" && 
                                                                               flightLog.takeoffLatitude != null && flightLog.takeoffLongitude != null &&
                                                                               flightLog.landingLatitude != null && flightLog.landingLongitude != null) {
                                                                        // Show coordinates view button when coordinates are available but text locations are not set
                                                                        button(classes = "btn btn-sm btn-outline-info") {
                                                                            attributes["onclick"] = "showCoordinatesModal(${flightLog.takeoffLatitude}, ${flightLog.takeoffLongitude}, ${flightLog.landingLatitude}, ${flightLog.landingLongitude}, '${flightLog.flightDate}')"
                                                                            +"🗺️ 地図で確認"
                                                                        }
                                                                        br()
                                                                        small(classes = "text-muted") { 
                                                                            +"📍 座標: (${flightLog.takeoffLatitude?.toPlainString()?.take(8)}, ${flightLog.takeoffLongitude?.toPlainString()?.take(8)}) → (${flightLog.landingLatitude?.toPlainString()?.take(8)}, ${flightLog.landingLongitude?.toPlainString()?.take(8)})"
                                                                        }
                                                                    } else {
                                                                        +(flightLog.takeoffLandingLocation ?: "未設定")
                                                                    }
                                                                }
                                                                td { 
                                                                    // Use new fields if available, fallback to legacy field
                                                                    val time = if (!flightLog.takeoffTime.isNullOrEmpty() && !flightLog.landingTime.isNullOrEmpty()) {
                                                                        "${flightLog.takeoffTime} - ${flightLog.landingTime}"
                                                                    } else {
                                                                        flightLog.takeoffLandingTime ?: "未設定"
                                                                    }
                                                                    +time
                                                                }
                                                                td { 
                                                                    // Use total flight time if available, fallback to flight duration
                                                                    val duration = flightLog.totalFlightTime ?: flightLog.flightDuration ?: "未設定"
                                                                    +duration
                                                                }
                                                                td { +flightLog.pilotName }
                                                                td(classes = "text-center") {
                                                                    a(href = "/flightlogs/ui/${flightLog.id}", classes = "btn btn-sm btn-outline-primary me-2") { +"編集" }
                                                                    form(action = "/flightlogs/ui/${flightLog.id}", method = FormMethod.post, classes = "d-inline") {
                                                                        hiddenInput { name = "_method"; value = "delete" }
                                                                        submitInput(classes = "btn btn-sm btn-outline-danger") { 
                                                                            value = "削除"
                                                                            attributes["onclick"] = "return confirm('本当に削除しますか？')"
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
                                    div(classes = "card-header") {
                                        h2(classes = "card-title mb-0") { +"新規飛行記録作成" }
                                    }
                                    div(classes = "card-body") {
                                        form(action = "/flightlogs/ui", method = FormMethod.post) {
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"飛行年月日" }
                                                    textInput(classes = "form-control") { 
                                                        name = "flightDate"
                                                        type = InputType.date
                                                        required = true
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"飛行させた者の氏名" }
                                                    textInput(classes = "form-control") { 
                                                        name = "pilotName"
                                                        placeholder = "操縦者名を入力してください"
                                                        required = true
                                                    }
                                                }
                                            }
                                            // Enhanced location input section
                                            div(classes = "row") {
                                                div(classes = "col-12 mb-4") {
                                                    div(classes = "card border-info") {
                                                        div(classes = "card-header bg-info text-white") {
                                                            h5(classes = "mb-0") { +"📍 離陸・着陸場所の入力方法" }
                                                        }
                                                        div(classes = "card-body") {
                                                            // Radio button selection
                                                            div(classes = "row mb-3") {
                                                                div(classes = "col-md-6") {
                                                                    div(classes = "form-check") {
                                                                        radioInput(classes = "form-check-input", name = "locationInputMethod") {
                                                                            value = "text"
                                                                            id = "inputMethodText"
                                                                            checked = true
                                                                            attributes["onchange"] = "toggleLocationInputMethod()"
                                                                        }
                                                                        label(classes = "form-check-label") {
                                                                            htmlFor = "inputMethodText"
                                                                            +"テキスト入力"
                                                                        }
                                                                    }
                                                                }
                                                                div(classes = "col-md-6") {
                                                                    div(classes = "form-check") {
                                                                        radioInput(classes = "form-check-input", name = "locationInputMethod") {
                                                                            value = "coordinates"
                                                                            id = "inputMethodCoordinates"
                                                                            attributes["onchange"] = "toggleLocationInputMethod()"
                                                                        }
                                                                        label(classes = "form-check-label") {
                                                                            htmlFor = "inputMethodCoordinates"
                                                                            +"地図上で座標入力"
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            
                                                            // Text input section
                                                            div(classes = "") {
                                                                id = "textInputSection"
                                                                div(classes = "row") {
                                                                    div(classes = "col-md-6 mb-3") {
                                                                        label(classes = "form-label") { +"離陸場所" }
                                                                        textInput(classes = "form-control", name = "takeoffLocation") { 
                                                                            id = "takeoffLocationText"
                                                                            placeholder = "離陸場所を入力してください"
                                                                        }
                                                                    }
                                                                    div(classes = "col-md-6 mb-3") {
                                                                        label(classes = "form-label") { +"着陸場所" }
                                                                        textInput(classes = "form-control", name = "landingLocation") { 
                                                                            id = "landingLocationText"
                                                                            placeholder = "着陸場所を入力してください"
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            
                                                            // Map input section
                                                            div(classes = "d-none") {
                                                                id = "mapInputSection"
                                                                div(classes = "row") {
                                                                    div(classes = "col-md-6 mb-3") {
                                                                        label(classes = "form-label") { +"離陸場所 (地図上でピンをドラッグ)" }
                                                                        div(classes = "border rounded") {
                                                                            id = "takeoffMap"
                                                                            style = "height: 300px; width: 100%;"
                                                                        }
                                                                        div(classes = "mt-2") {
                                                                            small(classes = "text-muted") { +"選択座標: " }
                                                                            span(classes = "") { 
                                                                                id = "takeoffCoordinatesDisplay"
                                                                                +"未選択" 
                                                                            }
                                                                        }
                                                                    }
                                                                    div(classes = "col-md-6 mb-3") {
                                                                        label(classes = "form-label") { +"着陸場所 (地図上でピンをドラッグ)" }
                                                                        div(classes = "border rounded") {
                                                                            id = "landingMap"
                                                                            style = "height: 300px; width: 100%;"
                                                                        }
                                                                        div(classes = "mt-2") {
                                                                            small(classes = "text-muted") { +"選択座標: " }
                                                                            span(classes = "") { 
                                                                                id = "landingCoordinatesDisplay"
                                                                                +"未選択" 
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            
                                                            // Hidden fields for coordinate data
                                                            hiddenInput(name = "takeoffInputType") { id = "takeoffInputType"; value = "text" }
                                                            hiddenInput(name = "landingInputType") { id = "landingInputType"; value = "text" }
                                                            hiddenInput(name = "takeoffLatitude") { id = "takeoffLatitude" }
                                                            hiddenInput(name = "takeoffLongitude") { id = "takeoffLongitude" }
                                                            hiddenInput(name = "landingLatitude") { id = "landingLatitude" }
                                                            hiddenInput(name = "landingLongitude") { id = "landingLongitude" }
                                                        }
                                                    }
                                                }
                                            }
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"離陸時刻" }
                                                    textInput(classes = "form-control") { 
                                                        name = "takeoffTime"
                                                        type = InputType.time
                                                        required = true
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"着陸時刻" }
                                                    textInput(classes = "form-control") { 
                                                        name = "landingTime"
                                                        type = InputType.time
                                                        required = true
                                                    }
                                                }
                                            }
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"総飛行時間" }
                                                    textInput(classes = "form-control") { 
                                                        name = "totalFlightTime"
                                                        placeholder = "例: 1時間30分 (自動計算される場合もあります)"
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"不具合やその対応（任意）" }
                                                    textInput(classes = "form-control") { 
                                                        name = "issuesAndResponses"
                                                        placeholder = "不具合があれば記載してください"
                                                    }
                                                }
                                            }
                                            div(classes = "mb-3") {
                                                label(classes = "form-label") { +"飛行概要（任意）" }
                                                textArea(classes = "form-control") { 
                                                    name = "flightSummary"
                                                    placeholder = "飛行の目的や概要を記載してください"
                                                    rows = "3"
                                                }
                                            }
                                            div(classes = "d-grid") {
                                                submitInput(classes = "btn btn-success") { value = "飛行記録を追加" }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                    
                    // Modal dialog for viewing coordinates on map
                    div(classes = "modal fade") {
                        attributes["id"] = "coordinatesModal"
                        attributes["tabindex"] = "-1"
                        attributes["aria-labelledby"] = "coordinatesModalLabel"
                        attributes["aria-hidden"] = "true"
                        div(classes = "modal-dialog modal-lg") {
                            div(classes = "modal-content") {
                                div(classes = "modal-header") {
                                    h5(classes = "modal-title") {
                                        attributes["id"] = "coordinatesModalLabel"
                                        +"🗺️ 離陸・着陸場所"
                                    }
                                    button(classes = "btn-close") {
                                        attributes["type"] = "button"
                                        attributes["data-bs-dismiss"] = "modal"
                                        attributes["aria-label"] = "Close"
                                    }
                                }
                                div(classes = "modal-body") {
                                    div(classes = "mb-3") {
                                        strong { +"飛行日: " }
                                        span {
                                            attributes["id"] = "modalFlightDate"
                                        }
                                    }
                                    div(classes = "row") {
                                        div(classes = "col-md-6 mb-3") {
                                            h6 { +"🛫 離陸場所" }
                                            div(classes = "text-muted mb-2") {
                                                small {
                                                    +"座標: "
                                                    span {
                                                        attributes["id"] = "modalTakeoffCoords"
                                                    }
                                                }
                                            }
                                        }
                                        div(classes = "col-md-6 mb-3") {
                                            h6 { +"🛬 着陸場所" }
                                            div(classes = "text-muted mb-2") {
                                                small {
                                                    +"座標: "
                                                    span {
                                                        attributes["id"] = "modalLandingCoords"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    div(classes = "border rounded") {
                                        attributes["id"] = "modalMap"
                                        style = "height: 400px; width: 100%;"
                                    }
                                }
                                div(classes = "modal-footer") {
                                    button(classes = "btn btn-secondary") {
                                        attributes["type"] = "button"
                                        attributes["data-bs-dismiss"] = "modal"
                                        +"閉じる"
                                    }
                                }
                            }
                        }
                    }
                    
                    // JavaScript for location input functionality
                    script {
                        unsafe {
                            +"""
                                // Global variables for maps
                                let takeoffMap, landingMap;
                                let takeoffMarker, landingMarker;
                                let modalMap;
                                
                                // Show coordinates modal with map
                                function showCoordinatesModal(takeoffLat, takeoffLng, landingLat, landingLng, flightDate) {
                                    // Update modal content
                                    document.getElementById('modalFlightDate').textContent = flightDate;
                                    document.getElementById('modalTakeoffCoords').textContent = takeoffLat.toFixed(6) + ', ' + takeoffLng.toFixed(6);
                                    document.getElementById('modalLandingCoords').textContent = landingLat.toFixed(6) + ', ' + landingLng.toFixed(6);
                                    
                                    // Show modal
                                    const modal = new bootstrap.Modal(document.getElementById('coordinatesModal'));
                                    modal.show();
                                    
                                    // Initialize map after modal is shown
                                    setTimeout(function() {
                                        initializeModalMap(takeoffLat, takeoffLng, landingLat, landingLng);
                                    }, 300);
                                }
                                
                                // Initialize modal map with both markers
                                function initializeModalMap(takeoffLat, takeoffLng, landingLat, landingLng) {
                                    // Remove existing map if any
                                    if (modalMap) {
                                        modalMap.remove();
                                    }
                                    
                                    // Calculate center point between takeoff and landing
                                    const centerLat = (parseFloat(takeoffLat) + parseFloat(landingLat)) / 2;
                                    const centerLng = (parseFloat(takeoffLng) + parseFloat(landingLng)) / 2;
                                    
                                    // Initialize map
                                    modalMap = L.map('modalMap').setView([centerLat, centerLng], 10);
                                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                        attribution: '© OpenStreetMap contributors'
                                    }).addTo(modalMap);
                                    
                                    // Add takeoff marker
                                    const takeoffMarker = L.marker([takeoffLat, takeoffLng]).addTo(modalMap);
                                    takeoffMarker.bindPopup('🛫 離陸場所<br>座標: ' + takeoffLat.toFixed(6) + ', ' + takeoffLng.toFixed(6));
                                    
                                    // Add landing marker
                                    const landingMarker = L.marker([landingLat, landingLng]).addTo(modalMap);
                                    landingMarker.bindPopup('🛬 着陸場所<br>座標: ' + landingLat.toFixed(6) + ', ' + landingLng.toFixed(6));
                                    
                                    // Draw line between takeoff and landing
                                    const flightPath = L.polyline([
                                        [takeoffLat, takeoffLng],
                                        [landingLat, landingLng]
                                    ], {color: 'blue', weight: 3, opacity: 0.7}).addTo(modalMap);
                                    
                                    // Auto-fit map to show both markers
                                    const group = new L.featureGroup([takeoffMarker, landingMarker, flightPath]);
                                    modalMap.fitBounds(group.getBounds(), {padding: [20, 20]});
                                }
                                
                                // Toggle between text and coordinate input methods
                                function toggleLocationInputMethod() {
                                    const textMethod = document.getElementById("inputMethodText").checked;
                                    const coordMethod = document.getElementById("inputMethodCoordinates").checked;
                                    
                                    const textSection = document.getElementById("textInputSection");
                                    const mapSection = document.getElementById("mapInputSection");
                                    
                                    if (textMethod) {
                                        textSection.classList.remove("d-none");
                                        mapSection.classList.add("d-none");
                                        document.getElementById("takeoffInputType").value = "text";
                                        document.getElementById("landingInputType").value = "text";
                                        // Clear coordinate fields
                                        document.getElementById("takeoffLatitude").value = "";
                                        document.getElementById("takeoffLongitude").value = "";
                                        document.getElementById("landingLatitude").value = "";
                                        document.getElementById("landingLongitude").value = "";
                                    } else if (coordMethod) {
                                        textSection.classList.add("d-none");
                                        mapSection.classList.remove("d-none");
                                        document.getElementById("takeoffInputType").value = "coordinates";
                                        document.getElementById("landingInputType").value = "coordinates";
                                        // Initialize maps
                                        setTimeout(initializeMaps, 100);
                                    }
                                }
                                
                                // Initialize Leaflet maps
                                function initializeMaps() {
                                    // Default location (Tokyo Station)
                                    const defaultLat = 35.6812;
                                    const defaultLng = 139.7671;
                                    
                                    // Initialize takeoff map
                                    if (takeoffMap) {
                                        takeoffMap.remove();
                                    }
                                    takeoffMap = L.map("takeoffMap").setView([defaultLat, defaultLng], 10);
                                    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
                                        attribution: "© OpenStreetMap contributors"
                                    }).addTo(takeoffMap);
                                    
                                    // Initialize landing map
                                    if (landingMap) {
                                        landingMap.remove();
                                    }
                                    landingMap = L.map("landingMap").setView([defaultLat, defaultLng], 10);
                                    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
                                        attribution: "© OpenStreetMap contributors"
                                    }).addTo(landingMap);
                                    
                                    // Add draggable markers
                                    takeoffMarker = L.marker([defaultLat, defaultLng], {draggable: true})
                                        .addTo(takeoffMap)
                                        .bindPopup("離陸場所<br>ドラッグして移動できます");
                                    
                                    landingMarker = L.marker([defaultLat, defaultLng], {draggable: true})
                                        .addTo(landingMap)
                                        .bindPopup("着陸場所<br>ドラッグして移動できます");
                                    
                                    // Update coordinates on marker drag
                                    takeoffMarker.on("dragend", function(e) {
                                        const position = e.target.getLatLng();
                                        document.getElementById("takeoffLatitude").value = position.lat.toFixed(6);
                                        document.getElementById("takeoffLongitude").value = position.lng.toFixed(6);
                                        document.getElementById("takeoffCoordinatesDisplay").textContent = 
                                            position.lat.toFixed(6) + ", " + position.lng.toFixed(6);
                                    });
                                    
                                    landingMarker.on("dragend", function(e) {
                                        const position = e.target.getLatLng();
                                        document.getElementById("landingLatitude").value = position.lat.toFixed(6);
                                        document.getElementById("landingLongitude").value = position.lng.toFixed(6);
                                        document.getElementById("landingCoordinatesDisplay").textContent = 
                                            position.lat.toFixed(6) + ", " + position.lng.toFixed(6);
                                    });
                                    
                                    // Set initial coordinates
                                    document.getElementById("takeoffLatitude").value = defaultLat.toFixed(6);
                                    document.getElementById("takeoffLongitude").value = defaultLng.toFixed(6);
                                    document.getElementById("landingLatitude").value = defaultLat.toFixed(6);
                                    document.getElementById("landingLongitude").value = defaultLng.toFixed(6);
                                    document.getElementById("takeoffCoordinatesDisplay").textContent = 
                                        defaultLat.toFixed(6) + ", " + defaultLng.toFixed(6);
                                    document.getElementById("landingCoordinatesDisplay").textContent = 
                                        defaultLat.toFixed(6) + ", " + defaultLng.toFixed(6);
                                }
                            """.trimIndent()
                        }
                    }
                }
            }
        }
        
        // Calendar view
        get("/calendar") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respondRedirect("/login")
                return@get
            }
            val flightLogs = flightLogService.getAllByUserId(session.userId)
            call.respondHtml {
                head { 
                    bootstrapHead("飛行記録 - カレンダー表示")
                    // Add FullCalendar CSS and JS
                    link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/fullcalendar@6.1.10/index.global.min.css")
                    script(src = "https://cdn.jsdelivr.net/npm/fullcalendar@6.1.10/index.global.min.js") {}
                }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    nav(classes = "navbar navbar-expand-lg navbar-dark bg-dark") {
                        div(classes = "container") {
                            a(href = "/", classes = "navbar-brand") { +"🛩️ OpenDroneDiary" }
                            div(classes = "navbar-nav ms-auto") {
                                span(classes = "navbar-text me-3") { +"ログイン中: ${session.username}" }
                                a(href = "/logout", classes = "btn btn-outline-light btn-sm") { +"ログアウト" }
                            }
                        }
                    }
                    div(classes = "container mt-4") {
                        div(classes = "row") {
                            div(classes = "col-12") {
                                div(classes = "card") {
                                    div(classes = "card-header d-flex justify-content-between align-items-center") {
                                        h1(classes = "card-title mb-0") { +"📅 飛行記録 - カレンダー表示" }
                                        div {
                                            a(href = "/flightlogs/ui", classes = "btn btn-outline-secondary btn-sm me-2") { +"📋 リスト表示" }
                                            a(href = "/flightlogs/ui/heatmap", classes = "btn btn-outline-warning btn-sm me-2") { +"🗺️ ヒートマップ" }
                                            a(href = "/flightlogs/ui/timeline", classes = "btn btn-outline-info btn-sm me-2") { +"📊 タイムライン表示" }
                                            a(href = "/", classes = "btn btn-outline-primary btn-sm") { +"トップへ" }
                                        }
                                    }
                                    div(classes = "card-body") {
                                        div { id = "calendar" }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                    
                    // FullCalendar initialization script
                    script {
                        unsafe {
                            +"""
                                document.addEventListener('DOMContentLoaded', function() {
                                    var calendarEl = document.getElementById('calendar');
                                    var events = [
                            """.trimIndent()
                        }
                        
                        // Generate events data from flight logs
                        flightLogs.forEachIndexed { index, flightLog ->
                            val title = if (!flightLog.takeoffLocation.isNullOrEmpty() && !flightLog.landingLocation.isNullOrEmpty()) {
                                "${flightLog.takeoffLocation} → ${flightLog.landingLocation}"
                            } else {
                                flightLog.takeoffLandingLocation ?: "飛行記録"
                            }
                            
                            val time = if (!flightLog.takeoffTime.isNullOrEmpty() && !flightLog.landingTime.isNullOrEmpty()) {
                                " (${flightLog.takeoffTime}-${flightLog.landingTime})"
                            } else if (!flightLog.takeoffLandingTime.isNullOrEmpty()) {
                                " (${flightLog.takeoffLandingTime})"
                            } else ""
                            
                            unsafe {
                                +"""
                                        {
                                            title: '${title}${time}',
                                            start: '${flightLog.flightDate}',
                                            url: '/flightlogs/ui/${flightLog.id}',
                                            backgroundColor: '#0d6efd',
                                            borderColor: '#0d6efd'
                                        }${if (index < flightLogs.size - 1) "," else ""}
                                """.trimIndent()
                            }
                        }
                        
                        unsafe {
                            +"""
                                    ];
                                    
                                    var calendar = new FullCalendar.Calendar(calendarEl, {
                                        initialView: 'dayGridMonth',
                                        headerToolbar: {
                                            left: 'prev,next today',
                                            center: 'title',
                                            right: 'dayGridMonth,listWeek'
                                        },
                                        events: events,
                                        locale: 'ja',
                                        height: 600,
                                        eventClick: function(info) {
                                            info.jsEvent.preventDefault();
                                            if (info.event.url) {
                                                window.open(info.event.url, '_self');
                                            }
                                        }
                                    });
                                    
                                    calendar.render();
                                });
                            """.trimIndent()
                        }
                    }
                }
            }
        }
        
        // Timeline view
        get("/timeline") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respondRedirect("/login")
                return@get
            }
            val flightLogs = flightLogService.getAllByUserId(session.userId)
                .sortedByDescending { it.flightDate } // Sort by date descending
            call.respondHtml {
                head { 
                    bootstrapHead("飛行記録 - タイムライン表示")
                    style {
                        unsafe {
                            +"""
                                .timeline {
                                    position: relative;
                                    padding-left: 30px;
                                }
                                
                                .timeline::before {
                                    content: '';
                                    position: absolute;
                                    left: 15px;
                                    top: 0;
                                    bottom: 0;
                                    width: 2px;
                                    background: #007bff;
                                }
                                
                                .timeline-item {
                                    position: relative;
                                    margin-bottom: 30px;
                                    padding-left: 25px;
                                }
                                
                                .timeline-item::before {
                                    content: '';
                                    position: absolute;
                                    left: -7px;
                                    top: 10px;
                                    width: 14px;
                                    height: 14px;
                                    border-radius: 50%;
                                    background: #007bff;
                                    border: 3px solid #fff;
                                    box-shadow: 0 0 0 2px #007bff;
                                }
                                
                                .flight-duration-bar {
                                    height: 20px;
                                    background: linear-gradient(90deg, #28a745 0%, #ffc107 50%, #dc3545 100%);
                                    border-radius: 10px;
                                    position: relative;
                                    margin: 10px 0;
                                }
                                
                                .flight-duration-text {
                                    position: absolute;
                                    top: 50%;
                                    left: 50%;
                                    transform: translate(-50%, -50%);
                                    color: white;
                                    font-weight: bold;
                                    font-size: 12px;
                                    text-shadow: 1px 1px 2px rgba(0,0,0,0.7);
                                }
                            """.trimIndent()
                        }
                    }
                }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    nav(classes = "navbar navbar-expand-lg navbar-dark bg-dark") {
                        div(classes = "container") {
                            a(href = "/", classes = "navbar-brand") { +"🛩️ OpenDroneDiary" }
                            div(classes = "navbar-nav ms-auto") {
                                span(classes = "navbar-text me-3") { +"ログイン中: ${session.username}" }
                                a(href = "/logout", classes = "btn btn-outline-light btn-sm") { +"ログアウト" }
                            }
                        }
                    }
                    div(classes = "container mt-4") {
                        div(classes = "row") {
                            div(classes = "col-12") {
                                div(classes = "card") {
                                    div(classes = "card-header d-flex justify-content-between align-items-center") {
                                        h1(classes = "card-title mb-0") { +"📊 飛行記録 - タイムライン表示" }
                                        div {
                                            a(href = "/flightlogs/ui", classes = "btn btn-outline-secondary btn-sm me-2") { +"📋 リスト表示" }
                                            a(href = "/flightlogs/ui/heatmap", classes = "btn btn-outline-warning btn-sm me-2") { +"🗺️ ヒートマップ" }
                                            a(href = "/flightlogs/ui/calendar", classes = "btn btn-outline-info btn-sm me-2") { +"📅 カレンダー表示" }
                                            a(href = "/", classes = "btn btn-outline-primary btn-sm") { +"トップへ" }
                                        }
                                    }
                                    div(classes = "card-body") {
                                        if (flightLogs.isEmpty()) {
                                            div(classes = "alert alert-info") { +"まだ飛行記録がありません。" }
                                        } else {
                                            div(classes = "timeline") {
                                                flightLogs.forEach { flightLog ->
                                                    div(classes = "timeline-item") {
                                                        div(classes = "card border-0 shadow-sm") {
                                                            div(classes = "card-header bg-light d-flex justify-content-between align-items-center") {
                                                                h5(classes = "mb-0") { 
                                                                    +"📅 ${flightLog.flightDate}"
                                                                }
                                                                span(classes = "badge bg-primary") { +"#${flightLog.id}" }
                                                            }
                                                            div(classes = "card-body") {
                                                                div(classes = "row") {
                                                                    div(classes = "col-md-6") {
                                                                        if (!flightLog.takeoffLocation.isNullOrEmpty() && !flightLog.landingLocation.isNullOrEmpty()) {
                                                                            p(classes = "mb-2") {
                                                                                strong { +"🛫 離陸場所: " }
                                                                                +flightLog.takeoffLocation
                                                                                if (flightLog.takeoffInputType == "coordinates" && 
                                                                                    flightLog.takeoffLatitude != null && flightLog.takeoffLongitude != null) {
                                                                                    br()
                                                                                    small(classes = "text-muted") { 
                                                                                        +"📍 (${flightLog.takeoffLatitude?.toPlainString()?.take(8)}, ${flightLog.takeoffLongitude?.toPlainString()?.take(8)})"
                                                                                    }
                                                                                }
                                                                            }
                                                                            p(classes = "mb-2") {
                                                                                strong { +"🛬 着陸場所: " }
                                                                                +flightLog.landingLocation
                                                                                if (flightLog.landingInputType == "coordinates" && 
                                                                                    flightLog.landingLatitude != null && flightLog.landingLongitude != null) {
                                                                                    br()
                                                                                    small(classes = "text-muted") { 
                                                                                        +"📍 (${flightLog.landingLatitude?.toPlainString()?.take(8)}, ${flightLog.landingLongitude?.toPlainString()?.take(8)})"
                                                                                    }
                                                                                }
                                                                            }
                                                                        } else if (!flightLog.takeoffLandingLocation.isNullOrEmpty()) {
                                                                            p(classes = "mb-2") {
                                                                                strong { +"📍 離着陸場所: " }
                                                                                +flightLog.takeoffLandingLocation
                                                                            }
                                                                        }
                                                                    }
                                                                    div(classes = "col-md-6") {
                                                                        if (!flightLog.takeoffTime.isNullOrEmpty() && !flightLog.landingTime.isNullOrEmpty()) {
                                                                            p(classes = "mb-2") {
                                                                                strong { +"🕐 離陸時刻: " }
                                                                                +flightLog.takeoffTime
                                                                            }
                                                                            p(classes = "mb-2") {
                                                                                strong { +"🕐 着陸時刻: " }
                                                                                +flightLog.landingTime
                                                                            }
                                                                        } else if (!flightLog.takeoffLandingTime.isNullOrEmpty()) {
                                                                            p(classes = "mb-2") {
                                                                                strong { +"🕐 時刻: " }
                                                                                +flightLog.takeoffLandingTime
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                // Flight duration visualization
                                                                val duration = flightLog.totalFlightTime ?: flightLog.flightDuration
                                                                if (!duration.isNullOrEmpty()) {
                                                                    div(classes = "flight-duration-bar") {
                                                                        div(classes = "flight-duration-text") {
                                                                            +"⏱️ $duration"
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                div(classes = "row mt-3") {
                                                                    div(classes = "col-md-6") {
                                                                        p(classes = "mb-2") {
                                                                            strong { +"👨‍✈️ 操縦者: " }
                                                                            +flightLog.pilotName
                                                                        }
                                                                    }
                                                                    div(classes = "col-md-6 text-end") {
                                                                        a(href = "/flightlogs/ui/${flightLog.id}", classes = "btn btn-sm btn-outline-primary") { 
                                                                            +"📝 編集" 
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                if (!flightLog.flightSummary.isNullOrEmpty()) {
                                                                    hr()
                                                                    p(classes = "mb-2") {
                                                                        strong { +"📋 飛行概要: " }
                                                                        br()
                                                                        +flightLog.flightSummary
                                                                    }
                                                                }
                                                                
                                                                if (!flightLog.issuesAndResponses.isNullOrEmpty()) {
                                                                    div(classes = "alert alert-warning mt-3 mb-0") {
                                                                        strong { +"⚠️ 不具合・対応: " }
                                                                        br()
                                                                        +flightLog.issuesAndResponses
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
                        }
                    }
                    addFooter()
                }
            }
        }
        
        // Heat map view
        get("/heatmap") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respondRedirect("/login")
                return@get
            }
            val flightLogs = flightLogService.getAllByUserId(session.userId)
                .filter { it.takeoffInputType == "coordinates" && it.landingInputType == "coordinates" &&
                         it.takeoffLatitude != null && it.takeoffLongitude != null &&
                         it.landingLatitude != null && it.landingLongitude != null }
            call.respondHtml {
                head { 
                    bootstrapHead("飛行記録 - ヒートマップ表示")
                    // Add Leaflet CSS and JS for heatmap functionality
                    link(rel = "stylesheet", href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css")
                    script(src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js") {}
                    script(src = "https://unpkg.com/leaflet.heat@0.2.0/dist/leaflet-heat.js") {}
                }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    nav(classes = "navbar navbar-expand-lg navbar-dark bg-dark") {
                        div(classes = "container") {
                            a(href = "/", classes = "navbar-brand") { +"🛩️ OpenDroneDiary" }
                            div(classes = "navbar-nav ms-auto") {
                                span(classes = "navbar-text me-3") { +"ログイン中: ${session.username}" }
                                a(href = "/logout", classes = "btn btn-outline-light btn-sm") { +"ログアウト" }
                            }
                        }
                    }
                    div(classes = "container mt-4") {
                        div(classes = "row") {
                            div(classes = "col-12") {
                                div(classes = "card") {
                                    div(classes = "card-header d-flex justify-content-between align-items-center") {
                                        h1(classes = "card-title mb-0") { +"🗺️ 飛行記録 - ヒートマップ表示" }
                                        div {
                                            a(href = "/flightlogs/ui", classes = "btn btn-outline-secondary btn-sm me-2") { +"📋 リスト表示" }
                                            a(href = "/flightlogs/ui/calendar", classes = "btn btn-outline-info btn-sm me-2") { +"📅 カレンダー表示" }
                                            a(href = "/flightlogs/ui/timeline", classes = "btn btn-outline-info btn-sm me-2") { +"📊 タイムライン表示" }
                                            a(href = "/", classes = "btn btn-outline-primary btn-sm") { +"トップへ" }
                                        }
                                    }
                                    div(classes = "card-body") {
                                        if (flightLogs.isEmpty()) {
                                            div(classes = "alert alert-warning") { 
                                                +"座標データが登録された飛行記録がありません。地図上で座標入力を行った飛行記録を作成すると、ここにヒートマップが表示されます。" 
                                            }
                                        } else {
                                            div(classes = "alert alert-info mb-3") {
                                                strong { +"表示データ: " }
                                                +"座標入力された飛行記録 ${flightLogs.size} 件のヒートマップ"
                                            }
                                        }
                                        div(classes = "") {
                                            id = "heatmapContainer"
                                            style = "height: 600px; width: 100%;"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                    
                    // Heatmap initialization script
                    script {
                        unsafe {
                            +"""
                                document.addEventListener('DOMContentLoaded', function() {
                                    const mapContainer = document.getElementById('heatmapContainer');
                                    
                                    // Default location (Tokyo Station)
                                    const defaultLat = 35.6812;
                                    const defaultLng = 139.7671;
                                    
                                    // Initialize map
                                    const map = L.map('heatmapContainer').setView([defaultLat, defaultLng], 10);
                                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                        attribution: '© OpenStreetMap contributors'
                                    }).addTo(map);
                                    
                                    // Prepare heat map data
                                    const heatData = [];
                                    const markers = [];
                            """.trimIndent()
                        }
                        
                        // Generate heat map data from flight logs with coordinates
                        flightLogs.forEach { flightLog ->
                            unsafe {
                                +"""
                                    // Add takeoff location
                                    heatData.push([${flightLog.takeoffLatitude}, ${flightLog.takeoffLongitude}, 0.8]);
                                    markers.push({
                                        lat: ${flightLog.takeoffLatitude},
                                        lng: ${flightLog.takeoffLongitude},
                                        type: 'takeoff',
                                        date: '${flightLog.flightDate}',
                                        pilot: '${flightLog.pilotName}'
                                    });
                                    
                                    // Add landing location
                                    heatData.push([${flightLog.landingLatitude}, ${flightLog.landingLongitude}, 0.8]);
                                    markers.push({
                                        lat: ${flightLog.landingLatitude},
                                        lng: ${flightLog.landingLongitude},
                                        type: 'landing',
                                        date: '${flightLog.flightDate}',
                                        pilot: '${flightLog.pilotName}'
                                    });
                                """.trimIndent()
                            }
                        }
                        
                        unsafe {
                            +"""
                                    // Add heat layer if we have data
                                    if (heatData.length > 0) {
                                        const heat = L.heatLayer(heatData, {
                                            radius: 25,
                                            blur: 15,
                                            maxZoom: 17,
                                            gradient: {0.4: 'blue', 0.6: 'cyan', 0.8: 'lime', 1.0: 'red'}
                                        }).addTo(map);
                                        
                                        // Add individual markers for detail
                                        markers.forEach(function(marker) {
                                            const icon = marker.type === 'takeoff' ? '🛫' : '🛬';
                                            const color = marker.type === 'takeoff' ? 'green' : 'red';
                                            
                                            L.marker([marker.lat, marker.lng]).addTo(map)
                                                .bindPopup(icon + ' ' + marker.type.charAt(0).toUpperCase() + marker.type.slice(1) + 
                                                          '<br>日付: ' + marker.date + 
                                                          '<br>操縦者: ' + marker.pilot);
                                        });
                                        
                                        // Auto-fit map to show all markers
                                        if (markers.length > 0) {
                                            const group = new L.featureGroup(map._layers);
                                            if (group.getBounds().isValid()) {
                                                map.fitBounds(group.getBounds(), {padding: [20, 20]});
                                            }
                                        }
                                    } else {
                                        // No coordinate data available, show default view
                                        L.marker([defaultLat, defaultLng]).addTo(map)
                                            .bindPopup('座標データが登録された飛行記録がありません<br>地図上で座標入力を行った飛行記録を作成してください')
                                            .openPopup();
                                    }
                                });
                            """.trimIndent()
                        }
                    }
                }
            }
        }
        
        get("/{id}") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respondRedirect("/login")
                return@get
            }
            val id = call.parameters["id"]?.toIntOrNull()
            val flightLog = id?.let { flightLogService.getByIdAndUserId(it, session.userId) }
            if (flightLog == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respondHtml {
                head { 
                    bootstrapHead("飛行記録編集")
                    // Add Leaflet CSS and JS for map functionality
                    link(rel = "stylesheet", href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css")
                    script(src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js") {}
                }
                body {
                    addGTMBodyScript()
                    nav(classes = "navbar navbar-expand-lg navbar-dark bg-dark") {
                        div(classes = "container") {
                            a(href = "/", classes = "navbar-brand") { +"🛩️ OpenDroneDiary" }
                            div(classes = "navbar-nav ms-auto") {
                                a(href = "/flightlogs/ui", classes = "btn btn-outline-light btn-sm me-2") { +"一覧へ戻る" }
                                a(href = "/logout", classes = "btn btn-outline-light btn-sm") { +"ログアウト" }
                            }
                        }
                    }
                    div(classes = "container mt-4") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-8") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"飛行記録編集" }
                                    }
                                    div(classes = "card-body") {
                                        form(action = "/flightlogs/ui/${flightLog.id}", method = FormMethod.post) {
                                            hiddenInput { name = "_method"; value = "put" }
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"飛行年月日" }
                                                    textInput(classes = "form-control") { 
                                                        name = "flightDate"
                                                        type = InputType.date
                                                        value = flightLog.flightDate
                                                        required = true
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"飛行させた者の氏名" }
                                                    textInput(classes = "form-control") { 
                                                        name = "pilotName"
                                                        value = flightLog.pilotName
                                                        required = true
                                                    }
                                                }
                                            }
                                            // Enhanced location input section
                                            div(classes = "row") {
                                                div(classes = "col-12 mb-4") {
                                                    div(classes = "card border-info") {
                                                        div(classes = "card-header bg-info text-white") {
                                                            h5(classes = "mb-0") { +"📍 離陸・着陸場所の入力方法" }
                                                        }
                                                        div(classes = "card-body") {
                                                            // Radio button selection
                                                            div(classes = "row mb-3") {
                                                                div(classes = "col-md-6") {
                                                                    div(classes = "form-check") {
                                                                        radioInput(classes = "form-check-input", name = "locationInputMethod") {
                                                                            value = "text"
                                                                            attributes["id"] = "inputMethodTextEdit"
                                                                            // Check if current flight log uses text input
                                                                            if (flightLog.takeoffInputType != "coordinates") checked = true
                                                                            attributes["onchange"] = "toggleLocationInputMethodEdit()"
                                                                        }
                                                                        label(classes = "form-check-label") {
                                                                            htmlFor = "inputMethodTextEdit"
                                                                            +"テキスト入力"
                                                                        }
                                                                    }
                                                                }
                                                                div(classes = "col-md-6") {
                                                                    div(classes = "form-check") {
                                                                        radioInput(classes = "form-check-input", name = "locationInputMethod") {
                                                                            value = "coordinates"
                                                                            attributes["id"] = "inputMethodCoordinatesEdit"
                                                                            // Check if current flight log uses coordinate input
                                                                            if (flightLog.takeoffInputType == "coordinates") checked = true
                                                                            attributes["onchange"] = "toggleLocationInputMethodEdit()"
                                                                        }
                                                                        label(classes = "form-check-label") {
                                                                            htmlFor = "inputMethodCoordinatesEdit"
                                                                            +"地図上で座標入力"
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            
                                                            // Text input section
                                                            div(classes = if (flightLog.takeoffInputType == "coordinates") "d-none" else "") {
                                                                attributes["id"] = "textInputSectionEdit"
                                                                div(classes = "row") {
                                                                    div(classes = "col-md-6 mb-3") {
                                                                        label(classes = "form-label") { +"離陸場所" }
                                                                        textInput(classes = "form-control", name = "takeoffLocation") { 
                                                                            attributes["id"] = "takeoffLocationTextEdit"
                                                                            placeholder = "離陸場所を入力してください"
                                                                            value = flightLog.takeoffLocation ?: flightLog.takeoffLandingLocation ?: ""
                                                                        }
                                                                    }
                                                                    div(classes = "col-md-6 mb-3") {
                                                                        label(classes = "form-label") { +"着陸場所" }
                                                                        textInput(classes = "form-control", name = "landingLocation") { 
                                                                            attributes["id"] = "landingLocationTextEdit"
                                                                            placeholder = "着陸場所を入力してください"
                                                                            value = flightLog.landingLocation ?: flightLog.takeoffLandingLocation ?: ""
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            
                                                            // Map input section
                                                            div(classes = if (flightLog.takeoffInputType != "coordinates") "d-none" else "") {
                                                                attributes["id"] = "mapInputSectionEdit"
                                                                div(classes = "row") {
                                                                    div(classes = "col-md-6 mb-3") {
                                                                        label(classes = "form-label") { +"離陸場所 (地図上でピンをドラッグ)" }
                                                                        div(classes = "border rounded") {
                                                                            attributes["id"] = "takeoffMapEdit"
                                                                            style = "height: 300px; width: 100%;"
                                                                        }
                                                                        div(classes = "mt-2") {
                                                                            small(classes = "text-muted") { +"選択座標: " }
                                                                            span(classes = "") { 
                                                                                attributes["id"] = "takeoffCoordinatesDisplayEdit"
                                                                                val coords = if (flightLog.takeoffLatitude != null && flightLog.takeoffLongitude != null) {
                                                                                    "${flightLog.takeoffLatitude?.toPlainString()?.take(8)}, ${flightLog.takeoffLongitude?.toPlainString()?.take(8)}"
                                                                                } else "未選択"
                                                                                +coords
                                                                            }
                                                                        }
                                                                    }
                                                                    div(classes = "col-md-6 mb-3") {
                                                                        label(classes = "form-label") { +"着陸場所 (地図上でピンをドラッグ)" }
                                                                        div(classes = "border rounded") {
                                                                            attributes["id"] = "landingMapEdit"
                                                                            style = "height: 300px; width: 100%;"
                                                                        }
                                                                        div(classes = "mt-2") {
                                                                            small(classes = "text-muted") { +"選択座標: " }
                                                                            span(classes = "") { 
                                                                                attributes["id"] = "landingCoordinatesDisplayEdit"
                                                                                val coords = if (flightLog.landingLatitude != null && flightLog.landingLongitude != null) {
                                                                                    "${flightLog.landingLatitude?.toPlainString()?.take(8)}, ${flightLog.landingLongitude?.toPlainString()?.take(8)}"
                                                                                } else "未選択"
                                                                                +coords
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            
                                                            // Hidden fields for coordinate data
                                                            hiddenInput(name = "takeoffInputType") { 
                                                                attributes["id"] = "takeoffInputTypeEdit"
                                                                value = flightLog.takeoffInputType 
                                                            }
                                                            hiddenInput(name = "landingInputType") { 
                                                                attributes["id"] = "landingInputTypeEdit"
                                                                value = flightLog.landingInputType 
                                                            }
                                                            hiddenInput(name = "takeoffLatitude") { 
                                                                attributes["id"] = "takeoffLatitudeEdit"
                                                                value = flightLog.takeoffLatitude?.toPlainString() ?: ""
                                                            }
                                                            hiddenInput(name = "takeoffLongitude") { 
                                                                attributes["id"] = "takeoffLongitudeEdit"
                                                                value = flightLog.takeoffLongitude?.toPlainString() ?: ""
                                                            }
                                                            hiddenInput(name = "landingLatitude") { 
                                                                attributes["id"] = "landingLatitudeEdit"
                                                                value = flightLog.landingLatitude?.toPlainString() ?: ""
                                                            }
                                                            hiddenInput(name = "landingLongitude") { 
                                                                attributes["id"] = "landingLongitudeEdit"
                                                                value = flightLog.landingLongitude?.toPlainString() ?: ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"離陸時刻" }
                                                    textInput(classes = "form-control") { 
                                                        name = "takeoffTime"
                                                        type = InputType.time
                                                        value = flightLog.takeoffTime ?: flightLog.takeoffLandingTime ?: ""
                                                        required = true
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"着陸時刻" }
                                                    textInput(classes = "form-control") { 
                                                        name = "landingTime"
                                                        type = InputType.time
                                                        value = flightLog.landingTime ?: flightLog.takeoffLandingTime ?: ""
                                                        required = true
                                                    }
                                                }
                                            }
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"総飛行時間" }
                                                    textInput(classes = "form-control") { 
                                                        name = "totalFlightTime"
                                                        value = flightLog.totalFlightTime ?: flightLog.flightDuration ?: ""
                                                        placeholder = "例: 1時間30分"
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"不具合やその対応（任意）" }
                                                    textInput(classes = "form-control") { 
                                                        name = "issuesAndResponses"
                                                        value = flightLog.issuesAndResponses ?: ""
                                                        placeholder = "不具合があれば記載してください"
                                                    }
                                                }
                                            }
                                            div(classes = "mb-3") {
                                                label(classes = "form-label") { +"飛行概要（任意）" }
                                                textArea(classes = "form-control") { 
                                                    name = "flightSummary"
                                                    placeholder = "飛行の目的や概要を記載してください"
                                                    rows = "3"
                                                    +(flightLog.flightSummary ?: "")
                                                }
                                            }
                                            div(classes = "d-grid gap-2 d-md-block") {
                                                submitInput(classes = "btn btn-primary") { value = "更新" }
                                            }
                                        }
                                        hr()
                                        form(action = "/flightlogs/ui/${flightLog.id}", method = FormMethod.post) {
                                            hiddenInput { name = "_method"; value = "delete" }
                                            div(classes = "d-grid") {
                                                submitInput(classes = "btn btn-danger") { 
                                                    value = "削除"
                                                    attributes["onclick"] = "return confirm('本当に削除しますか？')"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // JavaScript for location input functionality in edit form
                    script {
                        unsafe {
                            +"""
                                // Global variables for edit form maps
                                let takeoffMapEdit, landingMapEdit;
                                let takeoffMarkerEdit, landingMarkerEdit;
                                
                                // Toggle between text and coordinate input methods for edit form
                                function toggleLocationInputMethodEdit() {
                                    const textMethod = document.getElementById("inputMethodTextEdit").checked;
                                    const coordMethod = document.getElementById("inputMethodCoordinatesEdit").checked;
                                    
                                    const textSection = document.getElementById("textInputSectionEdit");
                                    const mapSection = document.getElementById("mapInputSectionEdit");
                                    
                                    if (textMethod) {
                                        textSection.classList.remove("d-none");
                                        mapSection.classList.add("d-none");
                                        document.getElementById("takeoffInputTypeEdit").value = "text";
                                        document.getElementById("landingInputTypeEdit").value = "text";
                                        // Clear coordinate fields when switching to text
                                        if (document.getElementById("takeoffLatitudeEdit").value === "" && document.getElementById("takeoffLongitudeEdit").value === "") {
                                            // Only clear if not already set
                                            document.getElementById("takeoffLatitudeEdit").value = "";
                                            document.getElementById("takeoffLongitudeEdit").value = "";
                                            document.getElementById("landingLatitudeEdit").value = "";
                                            document.getElementById("landingLongitudeEdit").value = "";
                                        }
                                    } else if (coordMethod) {
                                        textSection.classList.add("d-none");
                                        mapSection.classList.remove("d-none");
                                        document.getElementById("takeoffInputTypeEdit").value = "coordinates";
                                        document.getElementById("landingInputTypeEdit").value = "coordinates";
                                        // Initialize maps
                                        setTimeout(initializeEditMaps, 100);
                                    }
                                }
                                
                                // Initialize Leaflet maps for edit form
                                function initializeEditMaps() {
                                    // Get existing coordinates or use default location (Tokyo Station)
                                    const existingTakeoffLat = parseFloat(document.getElementById("takeoffLatitudeEdit").value) || 35.6812;
                                    const existingTakeoffLng = parseFloat(document.getElementById("takeoffLongitudeEdit").value) || 139.7671;
                                    const existingLandingLat = parseFloat(document.getElementById("landingLatitudeEdit").value) || 35.6812;
                                    const existingLandingLng = parseFloat(document.getElementById("landingLongitudeEdit").value) || 139.7671;
                                    
                                    // Initialize takeoff map
                                    if (takeoffMapEdit) {
                                        takeoffMapEdit.remove();
                                    }
                                    takeoffMapEdit = L.map("takeoffMapEdit").setView([existingTakeoffLat, existingTakeoffLng], 10);
                                    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
                                        attribution: "© OpenStreetMap contributors"
                                    }).addTo(takeoffMapEdit);
                                    
                                    // Initialize landing map
                                    if (landingMapEdit) {
                                        landingMapEdit.remove();
                                    }
                                    landingMapEdit = L.map("landingMapEdit").setView([existingLandingLat, existingLandingLng], 10);
                                    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
                                        attribution: "© OpenStreetMap contributors"
                                    }).addTo(landingMapEdit);
                                    
                                    // Add draggable markers with existing positions
                                    takeoffMarkerEdit = L.marker([existingTakeoffLat, existingTakeoffLng], {draggable: true})
                                        .addTo(takeoffMapEdit)
                                        .bindPopup("離陸場所<br>ドラッグして移動できます");
                                    
                                    landingMarkerEdit = L.marker([existingLandingLat, existingLandingLng], {draggable: true})
                                        .addTo(landingMapEdit)
                                        .bindPopup("着陸場所<br>ドラッグして移動できます");
                                    
                                    // Update coordinates on marker drag
                                    takeoffMarkerEdit.on("dragend", function(e) {
                                        const position = e.target.getLatLng();
                                        document.getElementById("takeoffLatitudeEdit").value = position.lat.toFixed(6);
                                        document.getElementById("takeoffLongitudeEdit").value = position.lng.toFixed(6);
                                        document.getElementById("takeoffCoordinatesDisplayEdit").textContent = 
                                            position.lat.toFixed(6) + ", " + position.lng.toFixed(6);
                                    });
                                    
                                    landingMarkerEdit.on("dragend", function(e) {
                                        const position = e.target.getLatLng();
                                        document.getElementById("landingLatitudeEdit").value = position.lat.toFixed(6);
                                        document.getElementById("landingLongitudeEdit").value = position.lng.toFixed(6);
                                        document.getElementById("landingCoordinatesDisplayEdit").textContent = 
                                            position.lat.toFixed(6) + ", " + position.lng.toFixed(6);
                                    });
                                    
                                    // Set initial coordinates in hidden fields if not already set
                                    if (!document.getElementById("takeoffLatitudeEdit").value) {
                                        document.getElementById("takeoffLatitudeEdit").value = existingTakeoffLat.toFixed(6);
                                        document.getElementById("takeoffLongitudeEdit").value = existingTakeoffLng.toFixed(6);
                                        document.getElementById("takeoffCoordinatesDisplayEdit").textContent = 
                                            existingTakeoffLat.toFixed(6) + ", " + existingTakeoffLng.toFixed(6);
                                    }
                                    if (!document.getElementById("landingLatitudeEdit").value) {
                                        document.getElementById("landingLatitudeEdit").value = existingLandingLat.toFixed(6);
                                        document.getElementById("landingLongitudeEdit").value = existingLandingLng.toFixed(6);
                                        document.getElementById("landingCoordinatesDisplayEdit").textContent = 
                                            existingLandingLat.toFixed(6) + ", " + existingLandingLng.toFixed(6);
                                    }
                                }
                                
                                // Initialize maps on page load if coordinate method is selected
                                document.addEventListener('DOMContentLoaded', function() {
                                    if (document.getElementById("inputMethodCoordinatesEdit").checked) {
                                        setTimeout(initializeEditMaps, 100);
                                    }
                                });
                            """.trimIndent()
                        }
                    }
                }
            }
        }
        // HTMLフォームからのPOSTリクエストをPUT/DELETE/POSTに振り分け
        post("/{id}") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respondRedirect("/login")
                return@post
            }
            val id = call.parameters["id"]?.toIntOrNull()
            val params = call.receiveParameters()
            val method = params["_method"]
            when (method) {
                "put" -> {
                    if (id != null) {
                        val flightDate = params["flightDate"] ?: ""
                        val pilotName = params["pilotName"] ?: ""
                        val issuesAndResponses = params["issuesAndResponses"]
                        
                        // Handle new fields
                        val takeoffLocation = params["takeoffLocation"]
                        val landingLocation = params["landingLocation"]
                        val takeoffTime = params["takeoffTime"]
                        val landingTime = params["landingTime"]
                        val flightSummary = params["flightSummary"]
                        val totalFlightTime = params["totalFlightTime"]
                        
                        // Legacy fields for backward compatibility  
                        val takeoffLandingLocation = params["takeoffLandingLocation"]
                        val takeoffLandingTime = params["takeoffLandingTime"]
                        val flightDuration = params["flightDuration"]
                        
                        // Handle coordinate fields
                        val takeoffInputType = params["takeoffInputType"] ?: "text"
                        val landingInputType = params["landingInputType"] ?: "text"
                        val takeoffLatitude = params["takeoffLatitude"]?.toBigDecimalOrNull()
                        val takeoffLongitude = params["takeoffLongitude"]?.toBigDecimalOrNull()
                        val landingLatitude = params["landingLatitude"]?.toBigDecimalOrNull()
                        val landingLongitude = params["landingLongitude"]?.toBigDecimalOrNull()
                        
                        val updated = flightLogService.update(id, FlightLog(
                            id = id, 
                            flightDate = flightDate, 
                            takeoffLandingLocation = takeoffLandingLocation, 
                            takeoffLandingTime = takeoffLandingTime, 
                            flightDuration = flightDuration, 
                            pilotName = pilotName, 
                            issuesAndResponses = issuesAndResponses, 
                            userId = session.userId,
                            takeoffLocation = takeoffLocation,
                            landingLocation = landingLocation,
                            takeoffTime = takeoffTime,
                            landingTime = landingTime,
                            flightSummary = flightSummary,
                            totalFlightTime = totalFlightTime,
                    takeoffInputType = takeoffInputType,
                    landingInputType = landingInputType,
                    takeoffLatitude = takeoffLatitude,
                    takeoffLongitude = takeoffLongitude,
                    landingLatitude = landingLatitude,
                    landingLongitude = landingLongitude
                        ), session.userId)
                        if (updated) {
                            call.respondRedirect("/flightlogs/ui")
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                "delete" -> {
                    if (id != null && flightLogService.delete(id, session.userId)) {
                        call.respondRedirect("/flightlogs/ui")
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                else -> call.respond(HttpStatusCode.BadRequest)
            }
        }
        // 新規作成フォーム用
        post {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respondRedirect("/login")
                return@post
            }
            val contentType = call.request.contentType()
            if (contentType.match(ContentType.Application.FormUrlEncoded)) {
                val params = call.receiveParameters()
                val flightDate = params["flightDate"] ?: ""
                val pilotName = params["pilotName"] ?: ""
                val issuesAndResponses = params["issuesAndResponses"]
                
                // Handle new fields
                val takeoffLocation = params["takeoffLocation"]
                val landingLocation = params["landingLocation"]
                val takeoffTime = params["takeoffTime"]
                val landingTime = params["landingTime"]
                val flightSummary = params["flightSummary"]
                val totalFlightTime = params["totalFlightTime"]
                
                // Legacy fields for backward compatibility
                val takeoffLandingLocation = params["takeoffLandingLocation"]
                val takeoffLandingTime = params["takeoffLandingTime"]
                val flightDuration = params["flightDuration"]
                
                // Handle coordinate fields
                val takeoffInputType = params["takeoffInputType"] ?: "text"
                val landingInputType = params["landingInputType"] ?: "text"
                val takeoffLatitude = params["takeoffLatitude"]?.toBigDecimalOrNull()
                val takeoffLongitude = params["takeoffLongitude"]?.toBigDecimalOrNull()
                val landingLatitude = params["landingLatitude"]?.toBigDecimalOrNull()
                val landingLongitude = params["landingLongitude"]?.toBigDecimalOrNull()
                
                val created = flightLogService.add(FlightLog(
                    id = 0, 
                    flightDate = flightDate, 
                    takeoffLandingLocation = takeoffLandingLocation, 
                    takeoffLandingTime = takeoffLandingTime, 
                    flightDuration = flightDuration, 
                    pilotName = pilotName, 
                    issuesAndResponses = issuesAndResponses, 
                    userId = session.userId,
                    takeoffLocation = takeoffLocation,
                    landingLocation = landingLocation,
                    takeoffTime = takeoffTime,
                    landingTime = landingTime,
                    flightSummary = flightSummary,
                    totalFlightTime = totalFlightTime,
                    takeoffInputType = takeoffInputType,
                    landingInputType = landingInputType,
                    takeoffLatitude = takeoffLatitude,
                    takeoffLongitude = takeoffLongitude,
                    landingLatitude = landingLatitude,
                    landingLongitude = landingLongitude
                ))
                call.respondRedirect("/flightlogs/ui")
            } else {
                val flightLog = call.receive<FlightLog>()
                val created = flightLogService.add(flightLog.copy(userId = session.userId))
                call.respond(HttpStatusCode.Created, created)
            }
        }
    }
}