package routing

import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.http.*
import com.opendronediary.model.FlightLog
import com.opendronediary.model.UserSession
import com.opendronediary.service.FlightLogService
import io.ktor.server.html.respondHtml
import kotlinx.html.*
import utils.GTMHelper.addGTMBodyScript
import utils.PolicyHelper.addFooter

fun Route.configureFlightLogRouting(flightLogService: FlightLogService) {
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
                val takeoffLandingLocation = params["takeoffLandingLocation"] ?: ""
                val takeoffLandingTime = params["takeoffLandingTime"] ?: ""
                val flightDuration = params["flightDuration"] ?: ""
                val pilotName = params["pilotName"] ?: ""
                val issuesAndResponses = params["issuesAndResponses"]
                val created = flightLogService.add(FlightLog(0, flightDate, takeoffLandingLocation, takeoffLandingTime, flightDuration, pilotName, issuesAndResponses, session.userId))
                call.respondRedirect("/flightlogs/ui")
            } else {
                val flightLog = call.receive<FlightLog>()
                val created = flightLogService.add(flightLog.copy(userId = session.userId))
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
                                        a(href = "/", classes = "btn btn-outline-primary btn-sm") { +"トップへ" }
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
                                                                td { +flightLog.takeoffLandingLocation }
                                                                td { +flightLog.takeoffLandingTime }
                                                                td { +flightLog.flightDuration }
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
                                        form(action = "/flightlogs", method = FormMethod.post) {
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
                                                    label(classes = "form-label") { +"離着陸場所" }
                                                    textInput(classes = "form-control") { 
                                                        name = "takeoffLandingLocation"
                                                        placeholder = "離着陸場所を入力してください"
                                                        required = true
                                                    }
                                                }
                                            }
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"離着陸時刻" }
                                                    textInput(classes = "form-control") { 
                                                        name = "takeoffLandingTime"
                                                        type = InputType.time
                                                        required = true
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"飛行時間" }
                                                    textInput(classes = "form-control") { 
                                                        name = "flightDuration"
                                                        placeholder = "例: 1時間30分"
                                                        required = true
                                                    }
                                                }
                                            }
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"飛行させた者の氏名" }
                                                    textInput(classes = "form-control") { 
                                                        name = "pilotName"
                                                        placeholder = "操縦者名を入力してください"
                                                        required = true
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
                                                    label(classes = "form-label") { +"離着陸場所" }
                                                    textInput(classes = "form-control") { 
                                                        name = "takeoffLandingLocation"
                                                        value = flightLog.takeoffLandingLocation
                                                        required = true
                                                    }
                                                }
                                            }
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"離着陸時刻" }
                                                    textInput(classes = "form-control") { 
                                                        name = "takeoffLandingTime"
                                                        type = InputType.time
                                                        value = flightLog.takeoffLandingTime
                                                        required = true
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"飛行時間" }
                                                    textInput(classes = "form-control") { 
                                                        name = "flightDuration"
                                                        value = flightLog.flightDuration
                                                        required = true
                                                    }
                                                }
                                            }
                                            div(classes = "row") {
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"飛行させた者の氏名" }
                                                    textInput(classes = "form-control") { 
                                                        name = "pilotName"
                                                        value = flightLog.pilotName
                                                        required = true
                                                    }
                                                }
                                                div(classes = "col-md-6 mb-3") {
                                                    label(classes = "form-label") { +"不具合やその対応（任意）" }
                                                    textInput(classes = "form-control") { 
                                                        name = "issuesAndResponses"
                                                        value = flightLog.issuesAndResponses ?: ""
                                                    }
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
                        val takeoffLandingLocation = params["takeoffLandingLocation"] ?: ""
                        val takeoffLandingTime = params["takeoffLandingTime"] ?: ""
                        val flightDuration = params["flightDuration"] ?: ""
                        val pilotName = params["pilotName"] ?: ""
                        val issuesAndResponses = params["issuesAndResponses"]
                        val updated = flightLogService.update(id, FlightLog(id, flightDate, takeoffLandingLocation, takeoffLandingTime, flightDuration, pilotName, issuesAndResponses, session.userId), session.userId)
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
                val takeoffLandingLocation = params["takeoffLandingLocation"] ?: ""
                val takeoffLandingTime = params["takeoffLandingTime"] ?: ""
                val flightDuration = params["flightDuration"] ?: ""
                val pilotName = params["pilotName"] ?: ""
                val issuesAndResponses = params["issuesAndResponses"]
                val created = flightLogService.add(FlightLog(0, flightDate, takeoffLandingLocation, takeoffLandingTime, flightDuration, pilotName, issuesAndResponses, session.userId))
                call.respondRedirect("/flightlogs/ui")
            } else {
                val flightLog = call.receive<FlightLog>()
                val created = flightLogService.add(flightLog.copy(userId = session.userId))
                call.respond(HttpStatusCode.Created, created)
            }
        }
    }
}