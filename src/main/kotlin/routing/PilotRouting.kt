package routing

import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.http.*
import com.opendronediary.model.Pilot
import com.opendronediary.model.UserSession
import com.opendronediary.service.PilotService
import io.ktor.server.html.respondHtml
import kotlinx.html.*
import utils.GTMHelper.addGTMBodyScript
import utils.PolicyHelper.addFooter
import routing.bootstrapHead

fun Route.configurePilotRouting(pilotService: PilotService) {
    // パイロット管理 API - Authentication required
    route("/pilots") {
        get {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            call.respond(pilotService.getAllByUserId(session.userId))
        }
        get("/{id}") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            val id = call.parameters["id"]?.toIntOrNull()
            val pilot = id?.let { pilotService.getByIdAndUserId(it, session.userId) }
            if (pilot != null) {
                call.respond(pilot)
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
                val name = params["name"]?.trim() ?: ""
                
                if (name.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "パイロット名は必須です")
                    return@post
                }
                
                val created = pilotService.add(Pilot(
                    id = 0,
                    name = name,
                    userId = session.userId,
                    skillCertificateNumber = params["skillCertificateNumber"].blankToNull()
                ))
                call.respondRedirect("/pilots/ui")
            } else {
                val pilot = call.receive<Pilot>()
                val created = pilotService.add(pilot.copy(userId = session.userId))
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
            val pilot = call.receive<Pilot>()
            if (id != null && pilotService.update(id, pilot, session.userId)) {
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
            if (id != null && pilotService.delete(id, session.userId)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    
    // パイロット管理 UI (HTML画面) - Authentication required
    route("/pilots/ui") {
        get {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respondRedirect("/login")
                return@get
            }
            val pilots = pilotService.getAllByUserId(session.userId)
            call.respondHtml {
                head { 
                    bootstrapHead("パイロット管理")
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
                                        h1(classes = "card-title mb-0") { +"👨‍✈️ パイロット管理" }
                                        div {
                                            a(href = "/flightlogs/ui", classes = "btn btn-outline-primary btn-sm me-2") { +"飛行記録へ" }
                                            a(href = "/", classes = "btn btn-outline-secondary btn-sm") { +"トップへ" }
                                        }
                                    }
                                    div(classes = "card-body") {
                                        if (pilots.isEmpty()) {
                                            div(classes = "alert alert-info") { +"まだパイロットが登録されていません。下のフォームから新規登録してください。" }
                                        } else {
                                            div(classes = "table-responsive") {
                                                table(classes = "table table-striped table-hover") {
                                                    thead(classes = "table-dark") {
                                                        tr {
                                                            th { +"ID" }
                                                            th { +"パイロット氏名" }
                                                            th { +"技能証明番号" }
                                                            th { +"登録日時" }
                                                            th(classes = "text-center") { +"操作" }
                                                        }
                                                    }
                                                    tbody {
                                                        pilots.forEach { pilot ->
                                                            tr {
                                                                td { +pilot.id.toString() }
                                                                td { 
                                                                    strong { +pilot.name }
                                                                }
                                                                td { +(pilot.skillCertificateNumber ?: "—") }
                                                                td { 
                                                                    pilot.createdAt?.let { 
                                                                        +it.toString().substring(0, 19).replace("T", " ")
                                                                    } ?: "不明" 
                                                                }
                                                                td(classes = "text-center") {
                                                                    a(href = "/pilots/ui/${pilot.id}", classes = "btn btn-sm btn-outline-primary me-2") { +"編集" }
                                                                    form(action = "/pilots/ui/${pilot.id}", method = FormMethod.post, classes = "d-inline") {
                                                                        hiddenInput { name = "_method"; value = "delete" }
                                                                        submitInput(classes = "btn btn-sm btn-outline-danger") { 
                                                                            value = "削除"
                                                                            attributes["onclick"] = "return confirm('本当に削除しますか？\\n\\n注意: このパイロットを参照している飛行記録がある場合、データに不整合が生じる可能性があります。')"
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
                                        h2(classes = "card-title mb-0") { +"新規パイロット登録" }
                                    }
                                    div(classes = "card-body") {
                                        form(action = "/pilots/ui", method = FormMethod.post) {
                                            div(classes = "row") {
                                                div(classes = "col-md-4 mb-3") {
                                                    label(classes = "form-label") { +"パイロット氏名" }
                                                    textInput(classes = "form-control") { 
                                                        name = "name"
                                                        placeholder = "パイロットの氏名を入力してください"
                                                        required = true
                                                        maxLength = "100"
                                                    }
                                                }
                                                div(classes = "col-md-4 mb-3") {
                                                    label(classes = "form-label") { +"技能証明番号（任意）" }
                                                    textInput(classes = "form-control") {
                                                        name = "skillCertificateNumber"
                                                        placeholder = "無人航空機操縦者技能証明書番号"
                                                        maxLength = "50"
                                                    }
                                                }
                                                div(classes = "col-md-4 mb-3 d-flex align-items-end") {
                                                    submitInput(classes = "btn btn-success w-100") { value = "パイロットを登録" }
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
            val pilot = id?.let { pilotService.getByIdAndUserId(it, session.userId) }
            if (pilot == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respondHtml {
                head { 
                    bootstrapHead("パイロット編集")
                }
                body {
                    addGTMBodyScript()
                    nav(classes = "navbar navbar-expand-lg navbar-dark bg-dark") {
                        div(classes = "container") {
                            a(href = "/", classes = "navbar-brand") { +"🛩️ OpenDroneDiary" }
                            div(classes = "navbar-nav ms-auto") {
                                a(href = "/pilots/ui", classes = "btn btn-outline-light btn-sm me-2") { +"一覧へ戻る" }
                                a(href = "/logout", classes = "btn btn-outline-light btn-sm") { +"ログアウト" }
                            }
                        }
                    }
                    div(classes = "container mt-4") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"パイロット編集" }
                                    }
                                    div(classes = "card-body") {
                                        form(action = "/pilots/ui/${pilot.id}", method = FormMethod.post) {
                                            hiddenInput { name = "_method"; value = "put" }
                                            div(classes = "mb-3") {
                                                label(classes = "form-label") { +"パイロット氏名" }
                                                textInput(classes = "form-control") { 
                                                    name = "name"
                                                    value = pilot.name
                                                    required = true
                                                    maxLength = "100"
                                                }
                                            }
                                            div(classes = "mb-3") {
                                                label(classes = "form-label") { +"技能証明番号（任意）" }
                                                textInput(classes = "form-control") {
                                                    name = "skillCertificateNumber"
                                                    value = pilot.skillCertificateNumber ?: ""
                                                    maxLength = "50"
                                                }
                                            }
                                            div(classes = "d-grid gap-2 d-md-block") {
                                                submitInput(classes = "btn btn-primary") { value = "更新" }
                                            }
                                        }
                                        hr()
                                        form(action = "/pilots/ui/${pilot.id}", method = FormMethod.post) {
                                            hiddenInput { name = "_method"; value = "delete" }
                                            div(classes = "d-grid") {
                                                submitInput(classes = "btn btn-danger") { 
                                                    value = "削除"
                                                    attributes["onclick"] = "return confirm('本当に削除しますか？\\n\\n注意: このパイロットを参照している飛行記録がある場合、データに不整合が生じる可能性があります。')"
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
                        val name = params["name"]?.trim() ?: ""
                        if (name.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "パイロット名は必須です")
                            return@post
                        }
                        val updated = pilotService.update(id, Pilot(
                            id = id,
                            name = name,
                            userId = session.userId,
                            skillCertificateNumber = params["skillCertificateNumber"].blankToNull()
                        ), session.userId)
                        if (updated) {
                            call.respondRedirect("/pilots/ui")
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                "delete" -> {
                    if (id != null && pilotService.delete(id, session.userId)) {
                        call.respondRedirect("/pilots/ui")
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
                val name = params["name"]?.trim() ?: ""
                
                if (name.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "パイロット名は必須です")
                    return@post
                }
                
                val created = pilotService.add(Pilot(
                    id = 0,
                    name = name,
                    userId = session.userId,
                    skillCertificateNumber = params["skillCertificateNumber"].blankToNull()
                ))
                call.respondRedirect("/pilots/ui")
            } else {
                val pilot = call.receive<Pilot>()
                val created = pilotService.add(pilot.copy(userId = session.userId))
                call.respond(HttpStatusCode.Created, created)
            }
        }
    }
}