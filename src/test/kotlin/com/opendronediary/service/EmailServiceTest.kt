package com.opendronediary.service

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EmailServiceTest {
    
    @Test
    fun testSendWelcomeEmailWithoutSmtpConfiguration() {
        val emailService = EmailService()
        
        // Without SMTP configuration, should return true (graceful handling)
        val result = emailService.sendWelcomeEmail("test@example.com", "testuser")
        assertTrue(result, "Welcome email should succeed even without SMTP configuration in development")
    }
    
    @Test
    fun testSendPasswordResetEmailWithoutSmtpConfiguration() {
        val emailService = EmailService()
        
        // Without SMTP configuration, should return true (graceful handling)
        val result = emailService.sendPasswordResetEmail("test@example.com", "dummy-token")
        assertTrue(result, "Password reset email should succeed even without SMTP configuration in development")
    }
}