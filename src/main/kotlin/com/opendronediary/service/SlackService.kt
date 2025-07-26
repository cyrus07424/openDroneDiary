package com.opendronediary.service

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class SlackMessage(
    val text: String,
    val username: String = "OpenDroneDiary",
    val icon_emoji: String = ":helicopter:"
)

class SlackService {
    private val logger = LoggerFactory.getLogger(SlackService::class.java)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    
    private val webhookUrl = System.getenv("SLACK_WEBHOOK_URL")
    
    suspend fun sendNotification(
        action: String,
        username: String?,
        userAgent: String?,
        ipAddress: String?,
        additionalInfo: String? = null
    ) {
        if (webhookUrl.isNullOrBlank()) {
            logger.debug("Slack webhook URL not configured, skipping notification")
            return
        }
        
        try {
            val message = buildMessage(action, username, userAgent, ipAddress, additionalInfo)
            
            client.post(webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody(SlackMessage(text = message))
            }
            
            logger.info("Slack notification sent for action: $action")
        } catch (e: Exception) {
            logger.error("Failed to send Slack notification for action: $action", e)
        }
    }
    
    private fun buildMessage(
        action: String,
        username: String?,
        userAgent: String?,
        ipAddress: String?,
        additionalInfo: String?
    ): String {
        val messageBuilder = StringBuilder()
        messageBuilder.append(":information_source: **$action**\n")
        
        if (!username.isNullOrBlank()) {
            messageBuilder.append("👤 ユーザー: $username\n")
        }
        
        if (!ipAddress.isNullOrBlank()) {
            messageBuilder.append("🌐 IPアドレス: $ipAddress\n")
        }
        
        if (!userAgent.isNullOrBlank()) {
            messageBuilder.append("🖥️ ユーザーエージェント: $userAgent\n")
        }
        
        if (!additionalInfo.isNullOrBlank()) {
            messageBuilder.append("📝 詳細: $additionalInfo\n")
        }
        
        return messageBuilder.toString()
    }
    
    fun close() {
        client.close()
    }
}