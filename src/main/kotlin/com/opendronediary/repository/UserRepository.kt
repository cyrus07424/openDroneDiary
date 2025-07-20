package com.opendronediary.repository

import com.opendronediary.model.User
import com.opendronediary.database.Users
import com.opendronediary.database.PasswordResetTokens
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class UserRepository {
    
    fun findByUsername(username: String): User? = transaction {
        Users.select { Users.username eq username }
            .map { User(it[Users.id], it[Users.username], it[Users.passwordHash], it[Users.email]) }
            .singleOrNull()
    }

    fun findByEmail(email: String): User? = transaction {
        Users.select { Users.email eq email }
            .map { User(it[Users.id], it[Users.username], it[Users.passwordHash], it[Users.email]) }
            .singleOrNull()
    }

    fun add(user: User): User = transaction {
        val insertedId = Users.insert {
            it[username] = user.username
            it[passwordHash] = user.passwordHash
            it[email] = user.email
        } get Users.id
        user.copy(id = insertedId)
    }

    fun getById(id: Int): User? = transaction {
        Users.select { Users.id eq id }
            .map { User(it[Users.id], it[Users.username], it[Users.passwordHash], it[Users.email]) }
            .singleOrNull()
    }
    
    fun updatePassword(userId: Int, newPasswordHash: String): Boolean = transaction {
        Users.update({ Users.id eq userId }) {
            it[passwordHash] = newPasswordHash
        } > 0
    }
    
    // Password reset token methods
    fun createPasswordResetToken(email: String, token: String, expiresAt: LocalDateTime): Boolean = transaction {
        PasswordResetTokens.insert {
            it[PasswordResetTokens.email] = email
            it[PasswordResetTokens.token] = token
            it[PasswordResetTokens.expiresAt] = expiresAt
        }
        true
    }
    
    fun findValidPasswordResetToken(token: String): String? = transaction {
        PasswordResetTokens.select { 
            (PasswordResetTokens.token eq token) and 
            (PasswordResetTokens.used eq false) and 
            (PasswordResetTokens.expiresAt greater LocalDateTime.now())
        }
            .map { it[PasswordResetTokens.email] }
            .singleOrNull()
    }
    
    fun markPasswordResetTokenAsUsed(token: String): Boolean = transaction {
        PasswordResetTokens.update({ PasswordResetTokens.token eq token }) {
            it[used] = true
        } > 0
    }
}