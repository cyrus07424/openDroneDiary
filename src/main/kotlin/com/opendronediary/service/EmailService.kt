package com.opendronediary.service

import javax.mail.*
import javax.mail.internet.*
import java.util.Properties

class EmailService {
    private val smtpHost = System.getenv("SMTP_HOST")
    private val smtpPort = System.getenv("SMTP_PORT") ?: "587"
    private val smtpUsername = System.getenv("SMTP_USERNAME")
    private val smtpPassword = System.getenv("SMTP_PASSWORD")
    private val fromEmail = System.getenv("SMTP_FROM_EMAIL") ?: "noreply@open-drone-diary.com"
    private val fromName = System.getenv("SMTP_FROM_NAME") ?: "OpenDroneDiary"
    private val smtpUseTLS = System.getenv("SMTP_USE_TLS")?.toBoolean() ?: true
    private val smtpUseSSL = System.getenv("SMTP_USE_SSL")?.toBoolean() ?: false
    private val baseUrl = System.getenv("BASE_URL") ?: "https://opendronediary.herokuapp.com"
    
    private fun createMailSession(): Session? {
        if (smtpHost.isNullOrEmpty() || smtpUsername.isNullOrEmpty() || smtpPassword.isNullOrEmpty()) {
            println("SMTP configuration not complete, skipping email send")
            return null
        }
        
        val props = Properties().apply {
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
            put("mail.smtp.auth", "true")
            if (smtpUseTLS) {
                put("mail.smtp.starttls.enable", "true")
            }
            if (smtpUseSSL) {
                put("mail.smtp.ssl.enable", "true")
            }
        }
        
        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUsername, smtpPassword)
            }
        })
    }
    
    private fun sendHtmlEmail(toEmail: String, subject: String, htmlContent: String): Boolean {
        val session = createMailSession() ?: return true // Return true for development without email
        
        return try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail, fromName))
                addRecipient(Message.RecipientType.TO, InternetAddress(toEmail))
                setSubject(subject, "UTF-8")
                setContent(htmlContent, "text/html; charset=utf-8")
            }
            
            Transport.send(message)
            true
        } catch (ex: Exception) {
            println("Error sending email: ${ex.message}")
            false
        }
    }
    
    fun sendWelcomeEmail(toEmail: String, username: String): Boolean {
        val subject = "OpenDroneDiary へようこそ！"
        
        val htmlContent = """
        <html>
        <body>
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h1 style="color: #0d6efd;">🛩️ OpenDroneDiary 🚁</h1>
                <h2>ユーザー登録が完了しました！</h2>
                <p>こんにちは、${username}さん</p>
                <p>OpenDroneDiary にご登録いただき、ありがとうございます。</p>
                <p>これで、ドローンの飛行日誌を管理できるようになりました。</p>
                <p><a href="${baseUrl}" style="background-color: #0d6efd; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">OpenDroneDiary にログイン</a></p>
                <p>ご不明な点がございましたら、お気軽にお問い合わせください。</p>
                <p>OpenDroneDiary チーム</p>
            </div>
        </body>
        </html>
        """.trimIndent()
        
        return sendHtmlEmail(toEmail, subject, htmlContent)
    }
    
    fun sendPasswordResetEmail(toEmail: String, token: String): Boolean {
        val subject = "パスワードのリセット - OpenDroneDiary"
        val resetUrl = "${baseUrl}/reset-password?token=${token}"
        
        val htmlContent = """
        <html>
        <body>
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h1 style="color: #0d6efd;">🛩️ OpenDroneDiary 🚁</h1>
                <h2>パスワードのリセット</h2>
                <p>パスワードのリセットが要求されました。</p>
                <p>パスワードをリセットするには、下記のリンクをクリックしてください。</p>
                <p><a href="${resetUrl}" style="background-color: #dc3545; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">パスワードをリセット</a></p>
                <p>このリンクは24時間有効です。</p>
                <p>このメールに心当たりがない場合は、無視してください。</p>
                <p>OpenDroneDiary チーム</p>
            </div>
        </body>
        </html>
        """.trimIndent()
        
        return sendHtmlEmail(toEmail, subject, htmlContent)
    }
}