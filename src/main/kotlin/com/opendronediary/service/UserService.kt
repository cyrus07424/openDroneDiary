package com.opendronediary.service

import com.opendronediary.model.User
import com.opendronediary.repository.UserRepository
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*

class UserService(private val repository: UserRepository) {
    
    fun register(username: String, password: String, email: String? = null): User? {
        if (repository.findByUsername(username) != null) {
            return null // User already exists
        }
        
        // Check if email is already used (if provided)
        if (!email.isNullOrEmpty() && repository.findByEmail(email) != null) {
            return null // Email already used
        }
        
        val passwordHash = hashPassword(password)
        return repository.add(User(0, username, passwordHash, email))
    }
    
    fun login(username: String, password: String): User? {
        val user = repository.findByUsername(username) ?: return null
        return if (verifyPassword(password, user.passwordHash)) user else null
    }
    
    fun getById(id: Int): User? = repository.getById(id)
    
    fun getUserByEmail(email: String): User? = repository.findByEmail(email)
    
    fun requestPasswordReset(email: String): String? {
        val user = repository.findByEmail(email) ?: return null
        
        // Generate secure token
        val token = generateSecureToken()
        val expiresAt = LocalDateTime.now().plusHours(24) // 24 hour expiry
        
        return if (repository.createPasswordResetToken(email, token, expiresAt)) {
            token
        } else {
            null
        }
    }
    
    fun resetPassword(token: String, newPassword: String): Boolean {
        val email = repository.findValidPasswordResetToken(token) ?: return false
        val user = repository.findByEmail(email) ?: return false
        
        val newPasswordHash = hashPassword(newPassword)
        val updated = repository.updatePassword(user.id, newPasswordHash)
        
        if (updated) {
            repository.markPasswordResetTokenAsUsed(token)
        }
        
        return updated
    }
    
    private fun hashPassword(password: String): String {
        // Simple hash for demo purposes - in real app use bcrypt or similar
        return password.hashCode().toString()
    }
    
    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
    
    private fun generateSecureToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}