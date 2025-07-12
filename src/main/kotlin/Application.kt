package com.example

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import com.opendronediary.database.DatabaseConfig

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    DatabaseConfig.initDatabase()
    configureSessions()
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
