package com.opendronediary.integration

import com.opendronediary.database.DatabaseConfig
import com.opendronediary.service.UserService
import com.opendronediary.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.junit.Test
import kotlin.test.*

class BcryptIntegrationTest {
    
    @Test
    fun testBcryptIntegration() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val password = "mySecurePassword123"
        val uniqueUsername = "bcryptuser_${System.currentTimeMillis()}"
        val uniqueEmail = "bcrypt_${System.currentTimeMillis()}@example.com"
        
        // Register user - this should use bcrypt
        val user = userService.register(uniqueUsername, password, uniqueEmail)
        assertNotNull(user, "User registration should succeed")
        
        // Verify the password hash is actually bcrypt formatted
        assertTrue(user.passwordHash.startsWith("$2"), "Password should be bcrypt hashed, got: ${user.passwordHash}")
        assertTrue(user.passwordHash.length > 50, "Bcrypt hash should be long")
        
        // Verify that the password is properly hashed
        assertNotEquals(password, user.passwordHash, "Password should not be stored in plain text")
        
        // Test that bcrypt can verify the password
        assertTrue(BCrypt.checkpw(password, user.passwordHash), "BCrypt should verify the password")
        assertFalse(BCrypt.checkpw("wrongpassword", user.passwordHash), "BCrypt should reject wrong password")
        
        // Test login functionality
        val loginUser = userService.login(uniqueUsername, password)
        assertNotNull(loginUser, "Login should succeed with correct password")
        assertEquals(uniqueUsername, loginUser.username)
        
        val loginUserWrong = userService.login(uniqueUsername, "wrongpassword")
        assertNull(loginUserWrong, "Login should fail with wrong password")
    }
    
    @Test
    fun testPasswordReset() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val originalPassword = "original123"
        val newPassword = "newPassword456"
        val uniqueUsername = "resettest_${System.currentTimeMillis()}"
        val uniqueEmail = "resettest_${System.currentTimeMillis()}@example.com"
        
        // Register user
        val user = userService.register(uniqueUsername, originalPassword, uniqueEmail)
        assertNotNull(user)
        
        val originalHash = user.passwordHash
        assertTrue(originalHash.startsWith("$2"), "Original password should be bcrypt hashed")
        
        // Request password reset
        val token = userService.requestPasswordReset(uniqueEmail)
        assertNotNull(token)
        
        // Reset password
        val success = userService.resetPassword(token, newPassword)
        assertTrue(success)
        
        // Verify new password works
        val loginNewPassword = userService.login(uniqueUsername, newPassword)
        assertNotNull(loginNewPassword)
        assertTrue(loginNewPassword.passwordHash.startsWith("$2"), "New password should be bcrypt hashed")
        assertNotEquals(originalHash, loginNewPassword.passwordHash, "Password hash should change after reset")
        
        // Verify old password doesn't work
        val loginOldPassword = userService.login(uniqueUsername, originalPassword)
        assertNull(loginOldPassword)
    }
}