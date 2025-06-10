package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.http.*
import com.opendronediary.model.Item
import com.opendronediary.model.User
import com.opendronediary.model.UserSession
import com.opendronediary.repository.ItemRepository
import com.opendronediary.repository.UserRepository
import com.opendronediary.service.ItemService
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
    val itemRepository = ItemRepository()
    val itemService = ItemService(itemRepository)
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
                                                a(href = "/items/ui", classes = "btn btn-primary") { +"Item一覧へ" }
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
        
        // Item CRUD - Authentication required
        route("/items") {
            get {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                call.respond(itemService.getAllByUserId(session.userId))
            }
            get("/{id}") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                val id = call.parameters["id"]?.toIntOrNull()
                val item = id?.let { itemService.getByIdAndUserId(it, session.userId) }
                if (item != null) {
                    call.respond(item)
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
                    val name = params["name"] ?: ""
                    val description = params["description"]
                    val created = itemService.add(Item(0, name, description, session.userId))
                    call.respondRedirect("/items/ui")
                } else {
                    val item = call.receive<Item>()
                    val created = itemService.add(item.copy(userId = session.userId))
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
                val item = call.receive<Item>()
                if (id != null && itemService.update(id, item, session.userId)) {
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
                if (id != null && itemService.delete(id, session.userId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        // Item UI (HTML画面) - Authentication required
        route("/items/ui") {
            get {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respondRedirect("/login")
                    return@get
                }
                val items = itemService.getAllByUserId(session.userId)
                call.respondHtml {
                    head { bootstrapHead("Item一覧") }
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
                                            h1(classes = "card-title mb-0") { +"Item一覧" }
                                            a(href = "/", classes = "btn btn-outline-primary btn-sm") { +"トップへ" }
                                        }
                                        div(classes = "card-body") {
                                            if (items.isEmpty()) {
                                                div(classes = "alert alert-info") { +"まだアイテムがありません。下のフォームから新規作成してください。" }
                                            } else {
                                                div(classes = "table-responsive") {
                                                    table(classes = "table table-striped table-hover") {
                                                        thead(classes = "table-dark") {
                                                            tr {
                                                                th { +"ID" }
                                                                th { +"名前" }
                                                                th { +"説明" }
                                                                th(classes = "text-center") { +"操作" }
                                                            }
                                                        }
                                                        tbody {
                                                            items.forEach { item ->
                                                                tr {
                                                                    td { +item.id.toString() }
                                                                    td { +item.name }
                                                                    td { +(item.description ?: "") }
                                                                    td(classes = "text-center") {
                                                                        a(href = "/items/ui/${item.id}", classes = "btn btn-sm btn-outline-primary me-2") { +"編集" }
                                                                        form(action = "/items/ui/${item.id}", method = FormMethod.post, classes = "d-inline") {
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
                                            h2(classes = "card-title mb-0") { +"新規作成" }
                                        }
                                        div(classes = "card-body") {
                                            form(action = "/items", method = FormMethod.post) {
                                                div(classes = "row") {
                                                    div(classes = "col-md-4 mb-3") {
                                                        label(classes = "form-label") { +"名前" }
                                                        textInput(classes = "form-control") { 
                                                            name = "name"
                                                            placeholder = "アイテム名を入力してください"
                                                            required = true
                                                        }
                                                    }
                                                    div(classes = "col-md-6 mb-3") {
                                                        label(classes = "form-label") { +"説明" }
                                                        textInput(classes = "form-control") { 
                                                            name = "description"
                                                            placeholder = "説明を入力してください（任意）"
                                                        }
                                                    }
                                                    div(classes = "col-md-2 mb-3") {
                                                        label(classes = "form-label") { +"　" } // spacer
                                                        div(classes = "d-grid") {
                                                            submitInput(classes = "btn btn-success") { value = "追加" }
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
            get("/{id}") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respondRedirect("/login")
                    return@get
                }
                val id = call.parameters["id"]?.toIntOrNull()
                val item = id?.let { itemService.getByIdAndUserId(it, session.userId) }
                if (item == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respondHtml {
                    head { bootstrapHead("Item編集") }
                    body {
                        nav(classes = "navbar navbar-expand-lg navbar-dark bg-dark") {
                            div(classes = "container") {
                                a(href = "/", classes = "navbar-brand") { +"🛩️ OpenDroneDiary" }
                                div(classes = "navbar-nav ms-auto") {
                                    a(href = "/items/ui", classes = "btn btn-outline-light btn-sm me-2") { +"一覧へ戻る" }
                                    a(href = "/logout", classes = "btn btn-outline-light btn-sm") { +"ログアウト" }
                                }
                            }
                        }
                        div(classes = "container mt-4") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-8") {
                                    div(classes = "card") {
                                        div(classes = "card-header") {
                                            h1(classes = "card-title mb-0") { +"Item編集" }
                                        }
                                        div(classes = "card-body") {
                                            form(action = "/items/ui/${item.id}", method = FormMethod.post) {
                                                hiddenInput { name = "_method"; value = "put" }
                                                div(classes = "mb-3") {
                                                    label(classes = "form-label") { +"名前" }
                                                    textInput(classes = "form-control") { 
                                                        name = "name"
                                                        value = item.name
                                                        required = true
                                                    }
                                                }
                                                div(classes = "mb-3") {
                                                    label(classes = "form-label") { +"説明" }
                                                    textInput(classes = "form-control") { 
                                                        name = "description"
                                                        value = item.description ?: ""
                                                    }
                                                }
                                                div(classes = "d-grid gap-2 d-md-block") {
                                                    submitInput(classes = "btn btn-primary") { value = "更新" }
                                                }
                                            }
                                            hr()
                                            form(action = "/items/ui/${item.id}", method = FormMethod.post) {
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
                            val name = params["name"] ?: ""
                            val description = params["description"]
                            val updated = itemService.update(id, Item(id, name, description, session.userId), session.userId)
                            if (updated) {
                                call.respondRedirect("/items/ui")
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                    "delete" -> {
                        if (id != null && itemService.delete(id, session.userId)) {
                            call.respondRedirect("/items/ui")
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
                    val name = params["name"] ?: ""
                    val description = params["description"]
                    val created = itemService.add(Item(0, name, description, session.userId))
                    call.respondRedirect("/items/ui")
                } else {
                    val item = call.receive<Item>()
                    val created = itemService.add(item.copy(userId = session.userId))
                    call.respond(HttpStatusCode.Created, created)
                }
            }
        }
    }
}
