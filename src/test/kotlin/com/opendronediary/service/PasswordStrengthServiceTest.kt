package com.opendronediary.service

import org.junit.Test
import kotlin.test.*

class PasswordStrengthServiceTest {
    
    private val passwordStrengthService = PasswordStrengthService()
    
    @Test
    fun testVeryWeakPassword() {
        val result = passwordStrengthService.validatePassword("123")
        assertFalse(result.isValid, "Very weak password should be rejected")
        assertTrue(result.score < 3, "Very weak password should have low score")
        assertTrue(result.feedback.contains("弱い"), "Should provide weakness feedback")
    }
    
    @Test
    fun testWeakPassword() {
        val result = passwordStrengthService.validatePassword("password")
        assertFalse(result.isValid, "Common weak password should be rejected")
        assertTrue(result.score < 3, "Weak password should have low score")
    }
    
    @Test
    fun testMediumPassword() {
        val result = passwordStrengthService.validatePassword("password123")
        // This might pass or fail depending on zxcvbn evaluation
        if (result.score < 3) {
            assertFalse(result.isValid, "Medium password with score < 3 should be rejected")
        }
    }
    
    @Test
    fun testStrongPassword() {
        val result = passwordStrengthService.validatePassword("MySecureP@ssw0rd2024!")
        assertTrue(result.isValid, "Strong password should be accepted")
        assertTrue(result.score >= 3, "Strong password should have high score")
        assertTrue(result.feedback.contains("良好"), "Should provide positive feedback")
    }
    
    @Test
    fun testPasswordWithUserInputs() {
        val username = "johndoe"
        val email = "john@example.com"
        
        // Password containing username should be weak
        val result1 = passwordStrengthService.validatePassword("johndoe123", listOf(username, email))
        assertFalse(result1.isValid, "Password containing username should be rejected")
        
        // Password containing part of email should be weak
        val result2 = passwordStrengthService.validatePassword("john123456", listOf(username, email))
        // This may or may not be rejected depending on zxcvbn's evaluation
        if (result2.score < 3) {
            assertFalse(result2.isValid, "Password containing email part should be rejected")
        }
    }
    
    @Test
    fun testComplexButPredictablePassword() {
        // Even complex passwords can be weak if they're predictable
        val result = passwordStrengthService.validatePassword("Password123!")
        // This is a common pattern and should likely be rejected
        if (result.score < 3) {
            assertFalse(result.isValid, "Predictable complex password should be rejected")
        }
    }
    
    @Test
    fun testRandomStrongPassword() {
        // A truly random strong password
        val result = passwordStrengthService.validatePassword("Xk9\$mN3#pQ7@wF2&")
        assertTrue(result.isValid, "Random strong password should be accepted")
        assertTrue(result.score >= 3, "Random strong password should have high score")
    }
}