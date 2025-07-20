package com.opendronediary.service

import com.opendronediary.database.DatabaseConfig
import com.opendronediary.model.User
import com.opendronediary.repository.UserRepository
import org.junit.Test
import kotlin.test.*

class UserServiceTest {
    
    @Test
    fun testPasswordHashingWithBcrypt() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val password = "testPassword123"
        val uniqueUsername = "testuser_${System.currentTimeMillis()}"
        val uniqueEmail = "test_${System.currentTimeMillis()}@example.com"
        
        // Register a user with bcrypt password hashing
        val user = userService.register(uniqueUsername, password, uniqueEmail)
        assertNotNull(user, "User registration should succeed")
        
        // Verify password hash is not the original password
        assertNotEquals(password, user.passwordHash)
        
        // Verify password hash is in bcrypt format (starts with $2a$, $2b$, etc.)
        assertTrue(user.passwordHash.startsWith("$2"), "Password hash should be in bcrypt format, got: ${user.passwordHash}")
        
        // Test login with correct password
        val loginUser = userService.login(uniqueUsername, password)
        assertNotNull(loginUser)
        assertEquals(user.username, loginUser.username)
        
        // Test login with incorrect password
        val loginUserWrongPassword = userService.login(uniqueUsername, "wrongPassword")
        assertNull(loginUserWrongPassword)
    }
    
    @Test
    fun testBackwardCompatibilityWithOldHashFormat() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        
        // Login should fail for old hash format (force re-registration)
        val oldHashUser = userService.login("nonexistent", "password")
        assertNull(oldHashUser)
    }
    
    @Test
    fun testPasswordReset() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val originalPassword = "originalPassword123"
        val newPassword = "newPassword456"
        val uniqueUsername = "resetuser_${System.currentTimeMillis()}"
        val uniqueEmail = "reset_${System.currentTimeMillis()}@example.com"
        
        // Register a user
        val user = userService.register(uniqueUsername, originalPassword, uniqueEmail)
        assertNotNull(user)
        
        // Request password reset
        val token = userService.requestPasswordReset(uniqueEmail)
        assertNotNull(token)
        
        // Reset password
        val success = userService.resetPassword(token, newPassword)
        assertTrue(success)
        
        // Verify old password no longer works
        val loginOldPassword = userService.login(uniqueUsername, originalPassword)
        assertNull(loginOldPassword)
        
        // Verify new password works
        val loginNewPassword = userService.login(uniqueUsername, newPassword)
        assertNotNull(loginNewPassword)
        assertEquals(user.username, loginNewPassword.username)
    }
}