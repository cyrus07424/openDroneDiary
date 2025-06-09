package com.opendronediary.repository

import com.opendronediary.model.User

class UserRepository {
    private val users = mutableListOf<User>()
    private var nextId = 1

    fun findByUsername(username: String): User? = users.find { it.username == username }

    fun add(user: User): User {
        val newUser = user.copy(id = nextId++)
        users.add(newUser)
        return newUser
    }

    fun getById(id: Int): User? = users.find { it.id == id }
}