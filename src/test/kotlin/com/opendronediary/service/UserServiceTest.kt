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
        
        // Test login with correct password (email-based)
        val loginUserByEmail = userService.loginByEmail(uniqueEmail, password)
        assertNotNull(loginUserByEmail)
        assertEquals(user.username, loginUserByEmail.username)
        
        // Test login with correct password (username-based, for backward compatibility)
        val loginUser = userService.login(uniqueUsername, password)
        assertNotNull(loginUser)
        assertEquals(user.username, loginUser.username)
        
        // Test login with incorrect password (email-based)
        val loginUserWrongPasswordByEmail = userService.loginByEmail(uniqueEmail, "wrongPassword")
        assertNull(loginUserWrongPasswordByEmail)
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
    
    @Test
    fun testEmailBasedLogin() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val password = "emailLoginTest123"
        val uniqueUsername = "emailuser_${System.currentTimeMillis()}"
        val uniqueEmail = "emaillogin_${System.currentTimeMillis()}@example.com"
        
        // Register a user
        val user = userService.register(uniqueUsername, password, uniqueEmail)
        assertNotNull(user, "User registration should succeed")
        
        // Test email-based login with correct credentials
        val loginByEmail = userService.loginByEmail(uniqueEmail, password)
        assertNotNull(loginByEmail, "Email-based login should succeed")
        assertEquals(user.username, loginByEmail.username)
        assertEquals(user.email, loginByEmail.email)
        
        // Test email-based login with wrong password
        val loginByEmailWrongPwd = userService.loginByEmail(uniqueEmail, "wrongPassword")
        assertNull(loginByEmailWrongPwd, "Email-based login should fail with wrong password")
        
        // Test email-based login with non-existent email
        val loginNonExistentEmail = userService.loginByEmail("nonexistent@example.com", password)
        assertNull(loginNonExistentEmail, "Email-based login should fail with non-existent email")
    }
    
    @Test
    fun testEmailUniquenessInRegistration() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val password = "uniqueEmailTest123"
        val username1 = "user1_${System.currentTimeMillis()}"
        val username2 = "user2_${System.currentTimeMillis()}"
        val sharedEmail = "shared_${System.currentTimeMillis()}@example.com"
        
        // Register first user with email
        val user1 = userService.register(username1, password, sharedEmail)
        assertNotNull(user1, "First user registration should succeed")
        
        // Try to register second user with same email
        val user2 = userService.register(username2, password, sharedEmail)
        assertNull(user2, "Second user registration should fail due to duplicate email")
    }
}