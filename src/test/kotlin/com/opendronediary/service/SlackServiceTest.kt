package com.opendronediary.service

import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class SlackServiceTest {
    
    @Test
    fun testSlackServiceWithoutWebhookUrl() {
        val slackService = SlackService()
        
        // This should not throw an exception and should log that webhook URL is not configured
        runBlocking {
            slackService.sendNotification(
                action = "Test action",
                username = "testuser",
                userAgent = "test-agent",
                ipAddress = "127.0.0.1",
                additionalInfo = "Test info"
            )
        }
        
        // If we get here without exception, the test passes
        println("Slack service handled missing webhook URL correctly")
    }
}