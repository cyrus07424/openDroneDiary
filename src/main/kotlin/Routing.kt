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

fun Application.configureRouting() {
    val itemRepository = ItemRepository()
    val itemService = ItemService(itemRepository)
    val userRepository = UserRepository()
    val userService = UserService(userRepository)
    
    routing {
        get("/") {
            val session = call.sessions.get<UserSession>()
            call.respondHtml {
                head { title { +"トップ" } }
                body {
                    h1 { +"Hello World!" }
                    if (session != null) {
                        p { +"ログイン中: ${session.username}" }
                        a(href = "/items/ui") { +"Item一覧へ" }
                        br
                        a(href = "/logout") { +"ログアウト" }
                    } else {
                        a(href = "/login") { +"ログイン" }
                        br
                        a(href = "/register") { +"ユーザー登録" }
                    }
                }
            }
        }
        
        // User authentication routes
        get("/login") {
            call.respondHtml {
                head { title { +"ログイン" } }
                body {
                    h1 { +"ログイン" }
                    form(action = "/login", method = FormMethod.post) {
                        textInput { name = "username"; placeholder = "ユーザー名" }
                        br
                        passwordInput { name = "password"; placeholder = "パスワード" }
                        br
                        submitInput { value = "ログイン" }
                    }
                    a(href = "/register") { +"ユーザー登録はこちら" }
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
                    head { title { +"ログインエラー" } }
                    body {
                        h1 { +"ログインに失敗しました" }
                        p { +"ユーザー名またはパスワードが間違っています。" }
                        a(href = "/login") { +"戻る" }
                    }
                }
            }
        }
        
        get("/register") {
            call.respondHtml {
                head { title { +"ユーザー登録" } }
                body {
                    h1 { +"ユーザー登録" }
                    form(action = "/register", method = FormMethod.post) {
                        textInput { name = "username"; placeholder = "ユーザー名" }
                        br
                        passwordInput { name = "password"; placeholder = "パスワード" }
                        br
                        submitInput { value = "登録" }
                    }
                    a(href = "/login") { +"ログインはこちら" }
                }
            }
        }
        
        post("/register") {
            val params = call.receiveParameters()
            val username = params["username"] ?: ""
            val password = params["password"] ?: ""
            
            if (username.isBlank() || password.isBlank()) {
                call.respondHtml(HttpStatusCode.BadRequest) {
                    head { title { +"登録エラー" } }
                    body {
                        h1 { +"入力エラー" }
                        p { +"ユーザー名とパスワードは必須です。" }
                        a(href = "/register") { +"戻る" }
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
                    head { title { +"登録エラー" } }
                    body {
                        h1 { +"ユーザー登録に失敗しました" }
                        p { +"そのユーザー名は既に使用されています。" }
                        a(href = "/register") { +"戻る" }
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
                    head {
                        title { +"Item一覧" }
                    }
                    body {
                        h1 { +"Item一覧 (${session.username})" }
                        a(href = "/") { +"トップへ" }
                        span { +" | " }
                        a(href = "/logout") { +"ログアウト" }
                        table {
                            tr {
                                th { +"ID" }
                                th { +"名前" }
                                th { +"説明" }
                                th { +"操作" }
                            }
                            items.forEach { item ->
                                tr {
                                    td { +item.id.toString() }
                                    td { +item.name }
                                    td { +(item.description ?: "") }
                                    td {
                                        a(href = "/items/ui/${item.id}") { +"編集" }
                                        form(action = "/items/ui/${item.id}", method = FormMethod.post) {
                                            attributes["style"] = "display:inline;"
                                            hiddenInput { name = "_method"; value = "delete" }
                                            submitInput { value = "削除" }
                                        }
                                    }
                                }
                            }
                        }
                        h2 { +"新規作成" }
                        form(action = "/items", method = FormMethod.post) {
                            textInput { name = "name"; placeholder = "名前" }
                            textInput { name = "description"; placeholder = "説明" }
                            submitInput { value = "追加" }
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
                    head { title { +"Item編集" } }
                    body {
                        h1 { +"Item編集" }
                        form(action = "/items/ui/${item.id}", method = FormMethod.post) {
                            hiddenInput { name = "_method"; value = "put" }
                            textInput { name = "name"; value = item.name }
                            textInput { name = "description"; value = item.description ?: "" }
                            submitInput { value = "更新" }
                        }
                        form(action = "/items/ui/${item.id}", method = FormMethod.post) {
                            hiddenInput { name = "_method"; value = "delete" }
                            submitInput { value = "削除" }
                        }
                        a(href = "/items/ui") { +"一覧へ戻る" }
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
