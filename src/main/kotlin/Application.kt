package com.example

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import com.opendronediary.database.DatabaseConfig
import utils.ErrorPageHelper

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    DatabaseConfig.initDatabase()
    configureSessions()
    configureStatusPages()
    configureRouting()
}

fun Application.configureSessions() {
    install(Sessions) {
        cookie<com.opendronediary.model.UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 24 * 60 * 60 // 24 hours
        }
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        // Handle general exceptions
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            ErrorPageHelper.respondWithSystemError(call)
        }
        
        // Handle specific HTTP status codes
        status(HttpStatusCode.InternalServerError) { status ->
            if (content.isEmptyErrorBody()) {
                ErrorPageHelper.respondWithSystemError(call)
            }
        }

        // ルートが既に説明付きの本文を返している場合は置き換えない。
        // 置き換えると、画像認証失敗などもすべて「リクエストエラー」になる。
        status(HttpStatusCode.BadRequest) { status ->
            if (content.isEmptyErrorBody()) {
                ErrorPageHelper.respondWithErrorPage(
                    call,
                    status,
                    "リクエストエラー",
                    "リクエストが正しくありません。入力内容を確認してください。"
                )
            }
        }

        status(HttpStatusCode.NotFound) { status ->
            if (content.isEmptyErrorBody()) {
                ErrorPageHelper.respondWithErrorPage(
                    call,
                    status,
                    "ページが見つかりません",
                    "お探しのページは存在しません。URLを確認してください。"
                )
            }
        }

        status(HttpStatusCode.Forbidden) { status ->
            if (content.isEmptyErrorBody()) {
                ErrorPageHelper.respondWithErrorPage(
                    call,
                    status,
                    "アクセス権限がありません",
                    "このページにアクセスする権限がありません。"
                )
            }
        }
    }
}

private fun OutgoingContent.isEmptyErrorBody(): Boolean {
    return when (this) {
        is OutgoingContent.NoContent -> true
        is OutgoingContent.ByteArrayContent -> bytes().isEmpty()
        else -> contentLength == 0L
    }
}
