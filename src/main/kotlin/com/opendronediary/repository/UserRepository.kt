package com.opendronediary.repository

import com.opendronediary.model.User
import com.opendronediary.database.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {
    
    fun findByUsername(username: String): User? = transaction {
        Users.select { Users.username eq username }
            .map { User(it[Users.id], it[Users.username], it[Users.passwordHash]) }
            .singleOrNull()
    }

    fun add(user: User): User = transaction {
        val insertedId = Users.insert {
            it[username] = user.username
            it[passwordHash] = user.passwordHash
        } get Users.id
        user.copy(id = insertedId)
    }

    fun getById(id: Int): User? = transaction {
        Users.select { Users.id eq id }
            .map { User(it[Users.id], it[Users.username], it[Users.passwordHash]) }
            .singleOrNull()
    }
}