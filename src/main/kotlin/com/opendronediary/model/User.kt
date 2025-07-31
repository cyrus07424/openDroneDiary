package com.opendronediary.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val email: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

@Serializable
data class UserSession(
    val userId: Int,
    val username: String
)