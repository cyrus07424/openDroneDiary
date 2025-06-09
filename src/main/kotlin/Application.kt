package com.example

import io.ktor.server.application.*
import io.ktor.server.sessions.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
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
