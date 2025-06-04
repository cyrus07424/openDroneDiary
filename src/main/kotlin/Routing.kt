package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.http.*
import com.opendronediary.model.Item
import com.opendronediary.repository.ItemRepository
import com.opendronediary.service.ItemService
import io.ktor.server.html.respondHtml
import kotlinx.html.*

fun Application.configureRouting() {
    val itemRepository = ItemRepository()
    val itemService = ItemService(itemRepository)
    routing {
        get("/") {
            call.respondHtml {
                head { title { +"トップ" } }
                body {
                    h1 { +"Hello World!" }
                    a(href = "/items/ui") { +"Item一覧へ" }
                }
            }
        }
        // Item CRUD
        route("/items") {
            get {
                call.respond(itemService.getAll())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val item = id?.let { itemService.getById(it) }
                if (item != null) {
                    call.respond(item)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            post {
                val contentType = call.request.contentType()
                if (contentType.match(ContentType.Application.FormUrlEncoded)) {
                    val params = call.receiveParameters()
                    val name = params["name"] ?: ""
                    val description = params["description"]
                    val created = itemService.add(Item(0, name, description))
                    call.respondRedirect("/items/ui")
                } else {
                    val item = call.receive<Item>()
                    val created = itemService.add(item)
                    call.respond(HttpStatusCode.Created, created)
                }
            }
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val item = call.receive<Item>()
                if (id != null && itemService.update(id, item)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id != null && itemService.delete(id)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        // Item UI (HTML画面)
        route("/items/ui") {
            get {
                val items = itemService.getAll()
                call.respondHtml {
                    head {
                        title { +"Item一覧" }
                    }
                    body {
                        h1 { +"Item一覧" }
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
                                        // ★テーブル側の削除ボタンも修正
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
                val id = call.parameters["id"]?.toIntOrNull()
                val item = id?.let { itemService.getById(it) }
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
                val id = call.parameters["id"]?.toIntOrNull()
                val params = call.receiveParameters()
                val method = params["_method"]
                when (method) {
                    "put" -> {
                        if (id != null) {
                            val name = params["name"] ?: ""
                            val description = params["description"]
                            val updated = itemService.update(id, Item(id, name, description))
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
                        if (id != null && itemService.delete(id)) {
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
                val contentType = call.request.contentType()
                if (contentType.match(ContentType.Application.FormUrlEncoded)) {
                    val params = call.receiveParameters()
                    val name = params["name"] ?: ""
                    val description = params["description"]
                    val created = itemService.add(Item(0, name, description))
                    call.respondRedirect("/items/ui")
                } else {
                    val item = call.receive<Item>()
                    val created = itemService.add(item)
                    call.respond(HttpStatusCode.Created, created)
                }
            }
        }
    }
}
