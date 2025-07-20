package com.opendronediary.model

import kotlinx.serialization.Serializable

data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val email: String? = null
)

@Serializable
data class UserSession(
    val userId: Int,
    val username: String
)