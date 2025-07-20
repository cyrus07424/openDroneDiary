package com.example

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
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
        status(HttpStatusCode.InternalServerError) { call, status ->
            ErrorPageHelper.respondWithSystemError(call)
        }
        
        status(HttpStatusCode.BadRequest) { call, status ->
            ErrorPageHelper.respondWithErrorPage(
                call,
                status,
                "リクエストエラー", 
                "リクエストが正しくありません。入力内容を確認してください。"
            )
        }
        
        status(HttpStatusCode.NotFound) { call, status ->
            ErrorPageHelper.respondWithErrorPage(
                call,
                status,
                "ページが見つかりません",
                "お探しのページは存在しません。URLを確認してください。"
            )
        }
        
        status(HttpStatusCode.Forbidden) { call, status ->
            ErrorPageHelper.respondWithErrorPage(
                call,
                status,
                "アクセス権限がありません",
                "このページにアクセスする権限がありません。"
            )
        }
    }
}
