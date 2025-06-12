package routing

import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.http.*
import com.opendronediary.model.DailyInspectionRecord
import com.opendronediary.model.UserSession
import com.opendronediary.service.DailyInspectionRecordService
import io.ktor.server.html.respondHtml
import kotlinx.html.*

fun Route.configureDailyInspectionRouting(dailyInspectionRecordService: DailyInspectionRecordService) {
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
}