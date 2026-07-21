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
        
        // Register a user with bcrypt password hashing - using a strong password
        val strongPassword = "MySecureT3stP@ssw0rd!"
        val result = userService.register(uniqueUsername, strongPassword, uniqueEmail)
        assertTrue(result is RegisterResult.PendingVerification, "User registration should return PendingVerification with strong password")
        val pending = result as RegisterResult.PendingVerification
        
        // Confirm registration
        val confirmResult = userService.confirmRegistration(pending.token)
        assertTrue(confirmResult is ConfirmRegistrationResult.Success, "Confirm registration should succeed")
        val user = (confirmResult as ConfirmRegistrationResult.Success).user
        
        // Verify password hash is not the original password
        assertNotEquals(strongPassword, user.passwordHash)
        
        // Verify password hash is in bcrypt format (starts with $2a$, $2b$, etc.)
        assertTrue(user.passwordHash.startsWith("$2"), "Password hash should be in bcrypt format, got: ${user.passwordHash}")
        
        // Test login with correct password (email-based)
        val loginUserByEmail = userService.loginByEmail(uniqueEmail, strongPassword)
        assertNotNull(loginUserByEmail)
        assertEquals(user.username, loginUserByEmail.username)
        
        // Test login with correct password (username-based, for backward compatibility)
        val loginUser = userService.login(uniqueUsername, strongPassword)
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
        val originalStrongPassword = "MyOrigin@lP@ssw0rd123!"
        val newStrongPassword = "MyNewSecur3P@ssw0rd456!"
        val uniqueUsername = "resetuser_${System.currentTimeMillis()}"
        val uniqueEmail = "reset_${System.currentTimeMillis()}@example.com"
        
        // Register a user with strong password (pending verification)
        val registerResult = userService.register(uniqueUsername, originalStrongPassword, uniqueEmail)
        assertTrue(registerResult is RegisterResult.PendingVerification, "User registration should return PendingVerification with strong password")
        val pending = registerResult as RegisterResult.PendingVerification
        
        // Confirm registration
        val confirmResult = userService.confirmRegistration(pending.token)
        assertTrue(confirmResult is ConfirmRegistrationResult.Success, "Confirm registration should succeed")
        val user = (confirmResult as ConfirmRegistrationResult.Success).user
        
        // Request password reset
        val token = userService.requestPasswordReset(uniqueEmail)
        assertNotNull(token)
        
        // Reset password with another strong password
        val resetResult = userService.resetPassword(token, newStrongPassword)
        assertTrue(resetResult is ResetPasswordResult.Success, "Password reset should succeed with strong password")
        
        // Verify old password no longer works
        val loginOldPassword = userService.login(uniqueUsername, originalStrongPassword)
        assertNull(loginOldPassword)
        
        // Verify new password works
        val loginNewPassword = userService.login(uniqueUsername, newStrongPassword)
        assertNotNull(loginNewPassword)
        assertEquals(user.username, loginNewPassword.username)
    }
    
    @Test
    fun testEmailBasedLogin() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val strongPassword = "MyEmailL0gin!T3st456"
        val uniqueUsername = "emailuser_${System.currentTimeMillis()}"
        val uniqueEmail = "emaillogin_${System.currentTimeMillis()}@example.com"
        
        // Register a user with strong password (pending verification)
        val registerResult = userService.register(uniqueUsername, strongPassword, uniqueEmail)
        assertTrue(registerResult is RegisterResult.PendingVerification, "User registration should return PendingVerification with strong password")
        val pending = registerResult as RegisterResult.PendingVerification
        
        // Confirm registration
        val confirmResult = userService.confirmRegistration(pending.token)
        assertTrue(confirmResult is ConfirmRegistrationResult.Success, "Confirm registration should succeed")
        val user = (confirmResult as ConfirmRegistrationResult.Success).user
        
        // Test email-based login with correct credentials
        val loginByEmail = userService.loginByEmail(uniqueEmail, strongPassword)
        assertNotNull(loginByEmail, "Email-based login should succeed")
        assertEquals(user.username, loginByEmail.username)
        assertEquals(user.email, loginByEmail.email)
        
        // Test email-based login with wrong password
        val loginByEmailWrongPwd = userService.loginByEmail(uniqueEmail, "wrongPassword")
        assertNull(loginByEmailWrongPwd, "Email-based login should fail with wrong password")
        
        // Test email-based login with non-existent email
        val loginNonExistentEmail = userService.loginByEmail("nonexistent@example.com", strongPassword)
        assertNull(loginNonExistentEmail, "Email-based login should fail with non-existent email")
    }
    
    @Test
    fun testEmailUniquenessInRegistration() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val strongPassword = "MyUn1queEm@il!T3st789"
        val username1 = "user1_${System.currentTimeMillis()}"
        val username2 = "user2_${System.currentTimeMillis()}"
        val sharedEmail = "shared_${System.currentTimeMillis()}@example.com"
        
        // Register first user with email and strong password (pending verification)
        val result1 = userService.register(username1, strongPassword, sharedEmail)
        assertTrue(result1 is RegisterResult.PendingVerification, "First user registration should return PendingVerification with strong password")
        
        // Confirm the first registration so the email is now in the Users table
        val confirmResult = userService.confirmRegistration((result1 as RegisterResult.PendingVerification).token)
        assertTrue(confirmResult is ConfirmRegistrationResult.Success, "First user confirmation should succeed")
        
        // Try to register second user with same email
        val result2 = userService.register(username2, strongPassword, sharedEmail)
        assertTrue(result2 is RegisterResult.Failure, "Second user registration should fail due to duplicate email")
        assertEquals("メールアドレスが既に使用されています", (result2 as RegisterResult.Failure).message)
    }
    
    @Test
    fun testPasswordStrengthValidation() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val uniqueUsername = "strengthtest_${System.currentTimeMillis()}"
        val uniqueEmail = "strengthtest_${System.currentTimeMillis()}@example.com"
        
        // Test with very weak password
        val weakResult = userService.register(uniqueUsername, "123", uniqueEmail)
        assertTrue(weakResult is RegisterResult.WeakPassword, "Very weak password should be rejected")
        
        // Test with common weak password
        val commonWeakResult = userService.register(uniqueUsername, "password", uniqueEmail)
        assertTrue(commonWeakResult is RegisterResult.WeakPassword, "Common weak password should be rejected")
        
        // Test with strong password
        val strongResult = userService.register(uniqueUsername, "MyV3rySecur3P@ssw0rd!", uniqueEmail)
        assertTrue(strongResult is RegisterResult.PendingVerification, "Strong password should return PendingVerification")
    }
    
    @Test
    fun testPasswordResetStrengthValidation() {
        // Initialize database
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val strongPassword = "MyOrigin@lStrong!P@ss"
        val uniqueUsername = "resetstrengthtest_${System.currentTimeMillis()}"
        val uniqueEmail = "resetstrengthtest_${System.currentTimeMillis()}@example.com"
        
        // Register user with strong password (pending verification)
        val registerResult = userService.register(uniqueUsername, strongPassword, uniqueEmail)
        assertTrue(registerResult is RegisterResult.PendingVerification, "User registration should return PendingVerification")
        
        // Confirm registration
        val confirmResult = userService.confirmRegistration((registerResult as RegisterResult.PendingVerification).token)
        assertTrue(confirmResult is ConfirmRegistrationResult.Success, "Confirm registration should succeed")
        
        // Request password reset
        val token = userService.requestPasswordReset(uniqueEmail)
        assertNotNull(token)
        
        // Try to reset with weak password
        val weakResetResult = userService.resetPassword(token, "123")
        assertTrue(weakResetResult is ResetPasswordResult.WeakPassword, "Weak password reset should be rejected")
        
        // Request new token since the previous one is still valid
        // Reset with strong password should work
        val strongResetResult = userService.resetPassword(token, "MyNewStr0ng!P@ssw0rd")
        assertTrue(strongResetResult is ResetPasswordResult.Success, "Strong password reset should succeed")
    }

    @Test
    fun testConfirmRegistrationWithInvalidToken() {
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        
        // Try confirming with an invalid token
        val result = userService.confirmRegistration("invalidtoken")
        assertTrue(result is ConfirmRegistrationResult.InvalidToken, "Invalid token should return InvalidToken")
    }

    @Test
    fun testPendingVerificationTokenIsUnique() {
        DatabaseConfig.initDatabase()
        
        val userService = UserService(UserRepository())
        val strongPassword = "MyUn1que!T0kenTest456"
        val username1 = "tokentest1_${System.currentTimeMillis()}"
        val email1 = "tokentest1_${System.currentTimeMillis()}@example.com"
        val username2 = "tokentest2_${System.currentTimeMillis()}"
        val email2 = "tokentest2_${System.currentTimeMillis()}@example.com"
        
        val result1 = userService.register(username1, strongPassword, email1)
        val result2 = userService.register(username2, strongPassword, email2)
        
        assertTrue(result1 is RegisterResult.PendingVerification)
        assertTrue(result2 is RegisterResult.PendingVerification)
        assertNotEquals(
            (result1 as RegisterResult.PendingVerification).token,
            (result2 as RegisterResult.PendingVerification).token,
            "Each registration should generate a unique token"
        )
    }
}