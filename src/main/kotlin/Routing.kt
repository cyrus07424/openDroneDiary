package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.http.*
import com.opendronediary.model.FlightLog
import com.opendronediary.model.DailyInspectionRecord
import com.opendronediary.model.MaintenanceInspectionRecord
import com.opendronediary.model.User
import com.opendronediary.model.UserSession
import com.opendronediary.repository.FlightLogRepository
import com.opendronediary.repository.DailyInspectionRecordRepository
import com.opendronediary.repository.MaintenanceInspectionRecordRepository
import com.opendronediary.repository.UserRepository
import com.opendronediary.service.FlightLogService
import com.opendronediary.service.DailyInspectionRecordService
import com.opendronediary.service.MaintenanceInspectionRecordService
import com.opendronediary.service.UserService
import io.ktor.server.html.respondHtml
import kotlinx.html.*

// Helper function to create Bootstrap head with CDN links
fun HEAD.bootstrapHead(pageTitle: String) {
    title { +pageTitle }
    meta(charset = "utf-8")
    meta(name = "viewport", content = "width=device-width, initial-scale=1")
    link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css")
    script(src = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js") { }
}

fun Application.configureRouting() {
    val flightLogRepository = FlightLogRepository()
    val flightLogService = FlightLogService(flightLogRepository)
    val dailyInspectionRecordRepository = DailyInspectionRecordRepository()
    val dailyInspectionRecordService = DailyInspectionRecordService(dailyInspectionRecordRepository)
    val maintenanceInspectionRecordRepository = MaintenanceInspectionRecordRepository()
    val maintenanceInspectionRecordService = MaintenanceInspectionRecordService(maintenanceInspectionRecordRepository)
    val userRepository = UserRepository()
    val userService = UserService(userRepository)
    
    routing {
        get("/") {
            val session = call.sessions.get<UserSession>()
            call.respondHtml {
                head { bootstrapHead("トップ") }
                body {
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-8") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"🛩️ OpenDroneDiary 🚁" }
                                    }
                                    div(classes = "card-body") {
                                        if (session != null) {
                                            div(classes = "alert alert-success") { +"ログイン中: ${session.username}" }
                                            div(classes = "d-grid gap-2") {
                                                a(href = "/flightlogs/ui", classes = "btn btn-primary") { +"飛行記録一覧へ" }
                                                a(href = "/dailyinspections/ui", classes = "btn btn-primary") { +"日常点検記録一覧へ" }
                                                a(href = "/maintenanceinspections/ui", classes = "btn btn-primary") { +"点検整備記録一覧へ" }
                                                a(href = "/logout", classes = "btn btn-outline-secondary") { +"ログアウト" }
                                            }
                                        } else {
                                            p(classes = "card-text") { +"ドローンの飛行日誌を管理するためのオープンソースのツールです。" }
                                            div(classes = "d-grid gap-2") {
                                                a(href = "/login", classes = "btn btn-primary") { +"ログイン" }
                                                a(href = "/register", classes = "btn btn-outline-primary") { +"ユーザー登録" }
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
        
        // User authentication routes
        get("/login") {
            call.respondHtml {
                head { bootstrapHead("ログイン") }
                body {
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"ログイン" }
                                    }
                                    div(classes = "card-body") {
                                        form(action = "/login", method = FormMethod.post) {
                                            div(classes = "mb-3") {
                                                label(classes = "form-label") { +"ユーザー名" }
                                                textInput(classes = "form-control") { 
                                                    name = "username"
                                                    placeholder = "ユーザー名を入力してください"
                                                    required = true
                                                }
                                            }
                                            div(classes = "mb-3") {
                                                label(classes = "form-label") { +"パスワード" }
                                                passwordInput(classes = "form-control") { 
                                                    name = "password"
                                                    placeholder = "パスワードを入力してください"
                                                    required = true
                                                }
                                            }
                                            div(classes = "d-grid") {
                                                submitInput(classes = "btn btn-primary") { value = "ログイン" }
                                            }
                                        }
                                        hr()
                                        div(classes = "text-center") {
                                            a(href = "/register", classes = "btn btn-link") { +"ユーザー登録はこちら" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        post("/login") {
            val params = call.receiveParameters()
            val username = params["username"] ?: ""
            val password = params["password"] ?: ""
            
            val user = userService.login(username, password)
            if (user != null) {
                call.sessions.set(UserSession(user.id, user.username))
                call.respondRedirect("/")
            } else {
                call.respondHtml(HttpStatusCode.Unauthorized) {
                    head { bootstrapHead("ログインエラー") }
                    body {
                        div(classes = "container mt-5") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-6") {
                                    div(classes = "card") {
                                        div(classes = "card-header") {
                                            h1(classes = "card-title mb-0") { +"ログインエラー" }
                                        }
                                        div(classes = "card-body") {
                                            div(classes = "alert alert-danger") {
                                                +"ユーザー名またはパスワードが間違っています。"
                                            }
                                            a(href = "/login", classes = "btn btn-primary") { +"戻る" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        get("/register") {
            call.respondHtml {
                head { bootstrapHead("ユーザー登録") }
                body {
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"ユーザー登録" }
                                    }
                                    div(classes = "card-body") {
                                        form(action = "/register", method = FormMethod.post) {
                                            div(classes = "mb-3") {
                                                label(classes = "form-label") { +"ユーザー名" }
                                                textInput(classes = "form-control") { 
                                                    name = "username"
                                                    placeholder = "ユーザー名を入力してください"
                                                    required = true
                                                }
                                            }
                                            div(classes = "mb-3") {
                                                label(classes = "form-label") { +"パスワード" }
                                                passwordInput(classes = "form-control") { 
                                                    name = "password"
                                                    placeholder = "パスワードを入力してください"
                                                    required = true
                                                }
                                            }
                                            div(classes = "d-grid") {
                                                submitInput(classes = "btn btn-success") { value = "登録" }
                                            }
                                        }
                                        hr()
                                        div(classes = "text-center") {
                                            a(href = "/login", classes = "btn btn-link") { +"ログインはこちら" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        post("/register") {
            val params = call.receiveParameters()
            val username = params["username"] ?: ""
            val password = params["password"] ?: ""
            
            if (username.isBlank() || password.isBlank()) {
                call.respondHtml(HttpStatusCode.BadRequest) {
                    head { bootstrapHead("登録エラー") }
                    body {
                        div(classes = "container mt-5") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-6") {
                                    div(classes = "card") {
                                        div(classes = "card-header") {
                                            h1(classes = "card-title mb-0") { +"入力エラー" }
                                        }
                                        div(classes = "card-body") {
                                            div(classes = "alert alert-danger") {
                                                +"ユーザー名とパスワードは必須です。"
                                            }
                                            a(href = "/register", classes = "btn btn-primary") { +"戻る" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return@post
            }
            
            val user = userService.register(username, password)
            if (user != null) {
                call.sessions.set(UserSession(user.id, user.username))
                call.respondRedirect("/")
            } else {
                call.respondHtml(HttpStatusCode.Conflict) {
                    head { bootstrapHead("登録エラー") }
                    body {
                        div(classes = "container mt-5") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-6") {
                                    div(classes = "card") {
                                        div(classes = "card-header") {
                                            h1(classes = "card-title mb-0") { +"登録エラー" }
                                        }
                                        div(classes = "card-body") {
                                            div(classes = "alert alert-warning") {
                                                +"そのユーザー名は既に使用されています。"
                                            }
                                            a(href = "/register", classes = "btn btn-primary") { +"戻る" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }
        
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
                    body {
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
        
        // Daily Inspection Record UI routes
        route("/dailyinspections/ui") {
            get {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respondRedirect("/login")
                    return@get
                }
                val dailyInspectionRecords = dailyInspectionRecordService.getAllByUserId(session.userId)
                call.respondHtml {
                    head { bootstrapHead("日常点検記録一覧") }
                    body {
                        div(classes = "container mt-5") {
                            div(classes = "row") {
                                div(classes = "col-12") {
                                    div(classes = "card") {
                                        div(classes = "card-header d-flex justify-content-between align-items-center") {
                                            h1(classes = "card-title mb-0") { +"日常点検記録一覧" }
                                            a(href = "/", classes = "btn btn-outline-primary btn-sm") { +"トップへ" }
                                        }
                                        div(classes = "card-body") {
                                            if (dailyInspectionRecords.isEmpty()) {
                                                div(classes = "alert alert-info") { +"まだ日常点検記録がありません。下のフォームから新規作成してください。" }
                                            } else {
                                                div(classes = "table-responsive") {
                                                    table(classes = "table table-striped table-hover") {
                                                        thead(classes = "table-dark") {
                                                            tr {
                                                                th { +"ID" }
                                                                th { +"点検日" }
                                                                th { +"場所" }
                                                                th { +"実施者" }
                                                                th { +"点検結果" }
                                                                th(classes = "text-center") { +"操作" }
                                                            }
                                                        }
                                                        tbody {
                                                            dailyInspectionRecords.forEach { record ->
                                                                tr {
                                                                    td { +"${record.id}" }
                                                                    td { +record.inspectionDate }
                                                                    td { +record.location }
                                                                    td { +record.inspectorName }
                                                                    td { +record.inspectionResult }
                                                                    td(classes = "text-center") {
                                                                        a(href = "/dailyinspections/ui/${record.id}", classes = "btn btn-sm btn-outline-primary me-2") { +"編集" }
                                                                        form(action = "/dailyinspections/ui/${record.id}", method = FormMethod.post, classes = "d-inline") {
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
                                            h2(classes = "card-title mb-0") { +"新規日常点検記録作成" }
                                        }
                                        div(classes = "card-body") {
                                            form(action = "/dailyinspections", method = FormMethod.post) {
                                                div(classes = "row") {
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"点検年月日" }
                                                        textInput(classes = "form-control") { 
                                                            name = "inspectionDate"
                                                            type = InputType.date
                                                            required = true
                                                        }
                                                    }
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"場所" }
                                                        textInput(classes = "form-control") { 
                                                            name = "location"
                                                            placeholder = "点検場所を入力してください"
                                                            required = true
                                                        }
                                                    }
                                                }
                                                div(classes = "row") {
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"実施者の氏名" }
                                                        textInput(classes = "form-control") { 
                                                            name = "inspectorName"
                                                            placeholder = "実施者の氏名を入力してください"
                                                            required = true
                                                        }
                                                    }
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"点検結果" }
                                                        textArea(classes = "form-control") { 
                                                            name = "inspectionResult"
                                                            placeholder = "点検結果を入力してください"
                                                            attributes["rows"] = "3"
                                                            required = true
                                                        }
                                                    }
                                                }
                                                div(classes = "d-grid") {
                                                    submitInput(classes = "btn btn-success") { value = "日常点検記録を追加" }
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
            get("/{id}") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respondRedirect("/login")
                    return@get
                }
                val id = call.parameters["id"]?.toIntOrNull()
                val record = id?.let { dailyInspectionRecordService.getByIdAndUserId(it, session.userId) }
                if (record == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respondHtml {
                    head { bootstrapHead("日常点検記録編集") }
                    body {
                        div(classes = "container mt-5") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-8") {
                                    div(classes = "card") {
                                        div(classes = "card-header d-flex justify-content-between align-items-center") {
                                            h1(classes = "card-title mb-0") { +"日常点検記録編集" }
                                            a(href = "/dailyinspections/ui", classes = "btn btn-outline-light btn-sm me-2") { +"一覧へ戻る" }
                                        }
                                        div(classes = "card-body") {
                                            form(action = "/dailyinspections/ui/${record.id}", method = FormMethod.post) {
                                                hiddenInput { name = "_method"; value = "put" }
                                                div(classes = "row") {
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"点検年月日" }
                                                        textInput(classes = "form-control") { 
                                                            name = "inspectionDate"
                                                            type = InputType.date
                                                            value = record.inspectionDate
                                                            required = true
                                                        }
                                                    }
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"場所" }
                                                        textInput(classes = "form-control") { 
                                                            name = "location"
                                                            value = record.location
                                                            required = true
                                                        }
                                                    }
                                                }
                                                div(classes = "row") {
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"実施者の氏名" }
                                                        textInput(classes = "form-control") { 
                                                            name = "inspectorName"
                                                            value = record.inspectorName
                                                            required = true
                                                        }
                                                    }
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"点検結果" }
                                                        textArea(classes = "form-control") { 
                                                            name = "inspectionResult"
                                                            attributes["rows"] = "3"
                                                            required = true
                                                            +record.inspectionResult
                                                        }
                                                    }
                                                }
                                                div(classes = "d-grid gap-2 d-md-block") {
                                                    submitInput(classes = "btn btn-primary") { value = "更新" }
                                                }
                                            }
                                            hr()
                                            form(action = "/dailyinspections/ui/${record.id}", method = FormMethod.post) {
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
            post("/{id}") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respondRedirect("/login")
                    return@post
                }
                val id = call.parameters["id"]?.toIntOrNull()
                val method = call.receiveParameters()["_method"]
                when (method) {
                    "put" -> {
                        val params = call.receiveParameters()
                        val inspectionDate = params["inspectionDate"] ?: ""
                        val location = params["location"] ?: ""
                        val inspectorName = params["inspectorName"] ?: ""
                        val inspectionResult = params["inspectionResult"] ?: ""
                        if (id != null && inspectionDate.isNotBlank() && location.isNotBlank() && 
                            inspectorName.isNotBlank() && inspectionResult.isNotBlank()) {
                            val updated = dailyInspectionRecordService.update(id, DailyInspectionRecord(id, inspectionDate, location, inspectorName, inspectionResult, session.userId), session.userId)
                            if (updated) {
                                call.respondRedirect("/dailyinspections/ui")
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                    "delete" -> {
                        if (id != null && dailyInspectionRecordService.delete(id, session.userId)) {
                            call.respondRedirect("/dailyinspections/ui")
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
                    val inspectionDate = params["inspectionDate"] ?: ""
                    val location = params["location"] ?: ""
                    val inspectorName = params["inspectorName"] ?: ""
                    val inspectionResult = params["inspectionResult"] ?: ""
                    val created = dailyInspectionRecordService.add(DailyInspectionRecord(0, inspectionDate, location, inspectorName, inspectionResult, session.userId))
                    call.respondRedirect("/dailyinspections/ui")
                } else {
                    val record = call.receive<DailyInspectionRecord>()
                    val created = dailyInspectionRecordService.add(record.copy(userId = session.userId))
                    call.respond(HttpStatusCode.Created, created)
                }
            }
        }
        
        // Daily Inspection Record routes
        route("/dailyinspections") {
            get {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                call.respond(dailyInspectionRecordService.getAllByUserId(session.userId))
            }
            get("/{id}") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                val id = call.parameters["id"]?.toIntOrNull()
                val record = id?.let { dailyInspectionRecordService.getByIdAndUserId(it, session.userId) }
                if (record != null) {
                    call.respond(record)
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
                    val inspectionDate = params["inspectionDate"] ?: ""
                    val location = params["location"] ?: ""
                    val inspectorName = params["inspectorName"] ?: ""
                    val inspectionResult = params["inspectionResult"] ?: ""
                    val created = dailyInspectionRecordService.add(DailyInspectionRecord(0, inspectionDate, location, inspectorName, inspectionResult, session.userId))
                    call.respondRedirect("/dailyinspections/ui")
                } else {
                    val record = call.receive<DailyInspectionRecord>()
                    val created = dailyInspectionRecordService.add(record.copy(userId = session.userId))
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
                val record = call.receive<DailyInspectionRecord>()
                if (id != null && dailyInspectionRecordService.update(id, record, session.userId)) {
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
                if (id != null && dailyInspectionRecordService.delete(id, session.userId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        
        // Maintenance Inspection Record UI routes
        route("/maintenanceinspections/ui") {
            get {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respondRedirect("/login")
                    return@get
                }
                val maintenanceInspectionRecords = maintenanceInspectionRecordService.getAllByUserId(session.userId)
                call.respondHtml {
                    head { bootstrapHead("点検整備記録一覧") }
                    body {
                        div(classes = "container mt-5") {
                            div(classes = "row") {
                                div(classes = "col-12") {
                                    div(classes = "card") {
                                        div(classes = "card-header d-flex justify-content-between align-items-center") {
                                            h1(classes = "card-title mb-0") { +"点検整備記録一覧" }
                                            a(href = "/", classes = "btn btn-outline-primary btn-sm") { +"トップへ" }
                                        }
                                        div(classes = "card-body") {
                                            if (maintenanceInspectionRecords.isEmpty()) {
                                                div(classes = "alert alert-info") { +"まだ点検整備記録がありません。下のフォームから新規作成してください。" }
                                            } else {
                                                div(classes = "table-responsive") {
                                                    table(classes = "table table-striped table-hover") {
                                                        thead(classes = "table-dark") {
                                                            tr {
                                                                th { +"ID" }
                                                                th { +"点検日" }
                                                                th { +"場所" }
                                                                th { +"実施者" }
                                                                th { +"内容・理由" }
                                                                th(classes = "text-center") { +"操作" }
                                                            }
                                                        }
                                                        tbody {
                                                            maintenanceInspectionRecords.forEach { record ->
                                                                tr {
                                                                    td { +"${record.id}" }
                                                                    td { +record.inspectionDate }
                                                                    td { +record.location }
                                                                    td { +record.inspectorName }
                                                                    td { +record.contentAndReason }
                                                                    td(classes = "text-center") {
                                                                        a(href = "/maintenanceinspections/ui/${record.id}", classes = "btn btn-sm btn-outline-primary me-2") { +"編集" }
                                                                        form(action = "/maintenanceinspections/ui/${record.id}", method = FormMethod.post, classes = "d-inline") {
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
                                            h2(classes = "card-title mb-0") { +"新規点検整備記録作成" }
                                        }
                                        div(classes = "card-body") {
                                            form(action = "/maintenanceinspections", method = FormMethod.post) {
                                                div(classes = "row") {
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"点検年月日" }
                                                        textInput(classes = "form-control") { 
                                                            name = "inspectionDate"
                                                            type = InputType.date
                                                            required = true
                                                        }
                                                    }
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"場所" }
                                                        textInput(classes = "form-control") { 
                                                            name = "location"
                                                            placeholder = "点検場所を入力してください"
                                                            required = true
                                                        }
                                                    }
                                                }
                                                div(classes = "row") {
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"実施者の氏名" }
                                                        textInput(classes = "form-control") { 
                                                            name = "inspectorName"
                                                            placeholder = "実施者の氏名を入力してください"
                                                            required = true
                                                        }
                                                    }
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"点検・修理・改造・整備の内容・理由" }
                                                        textArea(classes = "form-control") { 
                                                            name = "contentAndReason"
                                                            placeholder = "内容・理由を入力してください"
                                                            attributes["rows"] = "3"
                                                            required = true
                                                        }
                                                    }
                                                }
                                                div(classes = "d-grid") {
                                                    submitInput(classes = "btn btn-success") { value = "点検整備記録を追加" }
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
            get("/{id}") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respondRedirect("/login")
                    return@get
                }
                val id = call.parameters["id"]?.toIntOrNull()
                val record = id?.let { maintenanceInspectionRecordService.getByIdAndUserId(it, session.userId) }
                if (record == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respondHtml {
                    head { bootstrapHead("点検整備記録編集") }
                    body {
                        div(classes = "container mt-5") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-8") {
                                    div(classes = "card") {
                                        div(classes = "card-header d-flex justify-content-between align-items-center") {
                                            h1(classes = "card-title mb-0") { +"点検整備記録編集" }
                                            a(href = "/maintenanceinspections/ui", classes = "btn btn-outline-light btn-sm me-2") { +"一覧へ戻る" }
                                        }
                                        div(classes = "card-body") {
                                            form(action = "/maintenanceinspections/ui/${record.id}", method = FormMethod.post) {
                                                hiddenInput { name = "_method"; value = "put" }
                                                div(classes = "row") {
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"点検年月日" }
                                                        textInput(classes = "form-control") { 
                                                            name = "inspectionDate"
                                                            type = InputType.date
                                                            value = record.inspectionDate
                                                            required = true
                                                        }
                                                    }
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"場所" }
                                                        textInput(classes = "form-control") { 
                                                            name = "location"
                                                            value = record.location
                                                            required = true
                                                        }
                                                    }
                                                }
                                                div(classes = "row") {
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"実施者の氏名" }
                                                        textInput(classes = "form-control") { 
                                                            name = "inspectorName"
                                                            value = record.inspectorName
                                                            required = true
                                                        }
                                                    }
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"点検・修理・改造・整備の内容・理由" }
                                                        textArea(classes = "form-control") { 
                                                            name = "contentAndReason"
                                                            attributes["rows"] = "3"
                                                            required = true
                                                            +record.contentAndReason
                                                        }
                                                    }
                                                }
                                                div(classes = "d-grid gap-2 d-md-block") {
                                                    submitInput(classes = "btn btn-primary") { value = "更新" }
                                                }
                                            }
                                            hr()
                                            form(action = "/maintenanceinspections/ui/${record.id}", method = FormMethod.post) {
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
            post("/{id}") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respondRedirect("/login")
                    return@post
                }
                val id = call.parameters["id"]?.toIntOrNull()
                val method = call.receiveParameters()["_method"]
                when (method) {
                    "put" -> {
                        val params = call.receiveParameters()
                        val inspectionDate = params["inspectionDate"] ?: ""
                        val location = params["location"] ?: ""
                        val inspectorName = params["inspectorName"] ?: ""
                        val contentAndReason = params["contentAndReason"] ?: ""
                        if (id != null && inspectionDate.isNotBlank() && location.isNotBlank() && 
                            inspectorName.isNotBlank() && contentAndReason.isNotBlank()) {
                            val updated = maintenanceInspectionRecordService.update(id, MaintenanceInspectionRecord(id, inspectionDate, location, inspectorName, contentAndReason, session.userId), session.userId)
                            if (updated) {
                                call.respondRedirect("/maintenanceinspections/ui")
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                    "delete" -> {
                        if (id != null && maintenanceInspectionRecordService.delete(id, session.userId)) {
                            call.respondRedirect("/maintenanceinspections/ui")
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
                    val inspectionDate = params["inspectionDate"] ?: ""
                    val location = params["location"] ?: ""
                    val inspectorName = params["inspectorName"] ?: ""
                    val contentAndReason = params["contentAndReason"] ?: ""
                    val created = maintenanceInspectionRecordService.add(MaintenanceInspectionRecord(0, inspectionDate, location, inspectorName, contentAndReason, session.userId))
                    call.respondRedirect("/maintenanceinspections/ui")
                } else {
                    val record = call.receive<MaintenanceInspectionRecord>()
                    val created = maintenanceInspectionRecordService.add(record.copy(userId = session.userId))
                    call.respond(HttpStatusCode.Created, created)
                }
            }
        }
        
        // Maintenance Inspection Record routes
        route("/maintenanceinspections") {
            get {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                call.respond(maintenanceInspectionRecordService.getAllByUserId(session.userId))
            }
            get("/{id}") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                val id = call.parameters["id"]?.toIntOrNull()
                val record = id?.let { maintenanceInspectionRecordService.getByIdAndUserId(it, session.userId) }
                if (record != null) {
                    call.respond(record)
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
                    val inspectionDate = params["inspectionDate"] ?: ""
                    val location = params["location"] ?: ""
                    val inspectorName = params["inspectorName"] ?: ""
                    val contentAndReason = params["contentAndReason"] ?: ""
                    val created = maintenanceInspectionRecordService.add(MaintenanceInspectionRecord(0, inspectionDate, location, inspectorName, contentAndReason, session.userId))
                    call.respondRedirect("/maintenanceinspections/ui")
                } else {
                    val record = call.receive<MaintenanceInspectionRecord>()
                    val created = maintenanceInspectionRecordService.add(record.copy(userId = session.userId))
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
                val record = call.receive<MaintenanceInspectionRecord>()
                if (id != null && maintenanceInspectionRecordService.update(id, record, session.userId)) {
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
                if (id != null && maintenanceInspectionRecordService.delete(id, session.userId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
