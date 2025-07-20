package com.opendronediary.service

import com.sendgrid.*
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import java.io.IOException

class EmailService {
    private val apiKey = System.getenv("SENDGRID_API_KEY")
    private val fromEmail = System.getenv("SENDGRID_FROM_EMAIL") ?: "noreply@opendronediary.com"
    private val fromName = System.getenv("SENDGRID_FROM_NAME") ?: "OpenDroneDiary"
    private val baseUrl = System.getenv("BASE_URL") ?: "https://opendronediary.herokuapp.com"
    
    fun sendWelcomeEmail(toEmail: String, username: String): Boolean {
        if (apiKey.isNullOrEmpty()) {
            println("SendGrid API key not configured, skipping email send")
            return true // Return true for development without email
        }
        
        val from = Email(fromEmail, fromName)
        val to = Email(toEmail)
        val subject = "OpenDroneDiary へようこそ！"
        
        val content = Content(
            "text/html",
            """
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
        )
        
        val mail = Mail(from, subject, to, content)
        
        return try {
            val sg = SendGrid(apiKey)
            val request = Request().apply {
                method = Method.POST
                endpoint = "mail/send"
                body = mail.build()
            }
            val response = sg.api(request)
            response.statusCode in 200..299
        } catch (ex: IOException) {
            println("Error sending welcome email: ${ex.message}")
            false
        }
    }
    
    fun sendPasswordResetEmail(toEmail: String, token: String): Boolean {
        if (apiKey.isNullOrEmpty()) {
            println("SendGrid API key not configured, skipping email send")
            return true // Return true for development without email
        }
        
        val from = Email(fromEmail, fromName)
        val to = Email(toEmail)
        val subject = "パスワードのリセット - OpenDroneDiary"
        
        val resetUrl = "${baseUrl}/reset-password?token=${token}"
        
        val content = Content(
            "text/html",
            """
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
        )
        
        val mail = Mail(from, subject, to, content)
        
        return try {
            val sg = SendGrid(apiKey)
            val request = Request().apply {
                method = Method.POST
                endpoint = "mail/send"
                body = mail.build()
            }
            val response = sg.api(request)
            response.statusCode in 200..299
        } catch (ex: IOException) {
            println("Error sending password reset email: ${ex.message}")
            false
        }
    }
}