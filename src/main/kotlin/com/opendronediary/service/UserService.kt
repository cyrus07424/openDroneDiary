package com.opendronediary.service

import com.opendronediary.model.User
import com.opendronediary.repository.UserRepository

class UserService(private val repository: UserRepository) {
    
    fun register(username: String, password: String): User? {
        if (repository.findByUsername(username) != null) {
            return null // User already exists
        }
        val passwordHash = hashPassword(password)
        return repository.add(User(0, username, passwordHash))
    }
    
    fun login(username: String, password: String): User? {
        val user = repository.findByUsername(username) ?: return null
        return if (verifyPassword(password, user.passwordHash)) user else null
    }
    
    fun getById(id: Int): User? = repository.getById(id)
    
    private fun hashPassword(password: String): String {
        // Simple hash for demo purposes - in real app use bcrypt or similar
        return password.hashCode().toString()
    }
    
    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}