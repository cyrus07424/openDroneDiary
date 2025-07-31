package routing

import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.http.*
import com.opendronediary.model.FlightLog
import com.opendronediary.model.UserSession
import com.opendronediary.service.FlightLogService
import com.opendronediary.service.SlackService
import io.ktor.server.html.respondHtml
import kotlinx.html.*
import utils.GTMHelper.addGTMBodyScript
import utils.PolicyHelper.addFooter
import utils.RequestContextHelper

fun Route.configureFlightLogRouting(flightLogService: FlightLogService, slackService: SlackService) {
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
                    totalFlightTime = totalFlightTime
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
                head { bootstrapHead("飛行記録一覧") }
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
                                                                    val location = if (!flightLog.takeoffLocation.isNullOrEmpty() && !flightLog.landingLocation.isNullOrEmpty()) {
                                                                        "${flightLog.takeoffLocation} → ${flightLog.landingLocation}"
                                                                    } else {
                                                                        flightLog.takeoffLandingLocation ?: "未設定"
                                                                    }
                                                                    +location
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
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"離陸場所" }
                                                    textInput(classes = "form-control") { 
                                                        name = "takeoffLocation"
                                                        placeholder = "離陸場所を入力してください"
                                                        required = true
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"着陸場所" }
                                                    textInput(classes = "form-control") { 
                                                        name = "landingLocation"
                                                        placeholder = "着陸場所を入力してください"
                                                        required = true
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
                                                                            }
                                                                            p(classes = "mb-2") {
                                                                                strong { +"🛬 着陸場所: " }
                                                                                +flightLog.landingLocation
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
                head { bootstrapHead("飛行記録編集") }
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
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"離陸場所" }
                                                    textInput(classes = "form-control") { 
                                                        name = "takeoffLocation"
                                                        value = flightLog.takeoffLocation ?: flightLog.takeoffLandingLocation ?: ""
                                                        placeholder = "離陸場所を入力してください"
                                                        required = true
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"着陸場所" }
                                                    textInput(classes = "form-control") { 
                                                        name = "landingLocation"
                                                        value = flightLog.landingLocation ?: flightLog.takeoffLandingLocation ?: ""
                                                        placeholder = "着陸場所を入力してください"
                                                        required = true
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
                            totalFlightTime = totalFlightTime
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
                    totalFlightTime = totalFlightTime
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