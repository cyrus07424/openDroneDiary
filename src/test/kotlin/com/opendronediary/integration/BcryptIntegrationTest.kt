package com.opendronediary.integration

import com.opendronediary.database.DatabaseConfig
import com.opendronediary.service.UserService
import com.opendronediary.service.RegisterResult
import com.opendronediary.service.ResetPasswordResult
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
        val strongPassword = "MyVeryS3cur3Bcrypt!T3st"
        val uniqueUsername = "bcryptuser_${System.currentTimeMillis()}"
        val uniqueEmail = "bcrypt_${System.currentTimeMillis()}@example.com"
        
        // Register user - this should use bcrypt and strong password validation
        val result = userService.register(uniqueUsername, strongPassword, uniqueEmail)
        assertTrue(result is RegisterResult.Success, "User registration should succeed with strong password")
        val user = (result as RegisterResult.Success).user
        
        // Verify the password hash is actually bcrypt formatted
        assertTrue(user.passwordHash.startsWith("$2"), "Password should be bcrypt hashed, got: ${user.passwordHash}")
        assertTrue(user.passwordHash.length > 50, "Bcrypt hash should be long")
        
        // Verify that the password is properly hashed
        assertNotEquals(strongPassword, user.passwordHash, "Password should not be stored in plain text")
        
        // Test that bcrypt can verify the password
        assertTrue(BCrypt.checkpw(strongPassword, user.passwordHash), "BCrypt should verify the password")
        assertFalse(BCrypt.checkpw("wrongpassword", user.passwordHash), "BCrypt should reject wrong password")
        
        // Test login functionality
        val loginUser = userService.login(uniqueUsername, strongPassword)
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
        val originalStrongPassword = "MyOrigin@lBcrypt!T3st"
        val newStrongPassword = "MyNewBcrypt!P@ssw0rd456"
        val uniqueUsername = "resettest_${System.currentTimeMillis()}"
        val uniqueEmail = "resettest_${System.currentTimeMillis()}@example.com"
        
        // Register user with strong password
        val registerResult = userService.register(uniqueUsername, originalStrongPassword, uniqueEmail)
        assertTrue(registerResult is RegisterResult.Success, "User registration should succeed with strong password")
        val user = (registerResult as RegisterResult.Success).user
        
        val originalHash = user.passwordHash
        assertTrue(originalHash.startsWith("$2"), "Original password should be bcrypt hashed")
        
        // Request password reset
        val token = userService.requestPasswordReset(uniqueEmail)
        assertNotNull(token)
        
        // Reset password with another strong password
        val resetResult = userService.resetPassword(token, newStrongPassword)
        assertTrue(resetResult is ResetPasswordResult.Success, "Password reset should succeed with strong password")
        
        // Verify new password works
        val loginNewPassword = userService.login(uniqueUsername, newStrongPassword)
        assertNotNull(loginNewPassword)
        assertTrue(loginNewPassword.passwordHash.startsWith("$2"), "New password should be bcrypt hashed")
        assertNotEquals(originalHash, loginNewPassword.passwordHash, "Password hash should change after reset")
        
        // Verify old password doesn't work
        val loginOldPassword = userService.login(uniqueUsername, originalStrongPassword)
        assertNull(loginOldPassword)
    }
}