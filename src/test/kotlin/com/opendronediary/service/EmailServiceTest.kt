package com.opendronediary.service

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EmailServiceTest {
    
    @Test
    fun testSendWelcomeEmailWithoutApiKey() {
        val emailService = EmailService()
        
        // Without API key configured, should return true (graceful handling)
        val result = emailService.sendWelcomeEmail("test@example.com", "testuser")
        assertTrue(result, "Welcome email should succeed even without API key in development")
    }
    
    @Test
    fun testSendPasswordResetEmailWithoutApiKey() {
        val emailService = EmailService()
        
        // Without API key configured, should return true (graceful handling)
        val result = emailService.sendPasswordResetEmail("test@example.com", "dummy-token")
        assertTrue(result, "Password reset email should succeed even without API key in development")
    }
}