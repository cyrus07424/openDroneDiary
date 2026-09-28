package com.opendronediary.service

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

class CaptchaService(
    private val challengeTtl: Duration = Duration.ofMinutes(10),
    private val random: SecureRandom = SecureRandom()
) {
    private val challenges = ConcurrentHashMap<String, CaptchaChallenge>()

    fun createChallenge(): CaptchaChallengeData {
        cleanupExpiredChallenges()
        val answer = generateAnswer()
        val challengeId = UUID.randomUUID().toString()
        challenges[challengeId] = CaptchaChallenge(
            answer = answer,
            normalizedAnswer = answer.lowercase(Locale.ROOT),
            expiresAt = Instant.now().plus(challengeTtl)
        )
        return CaptchaChallengeData(challengeId)
    }

    fun renderChallenge(challengeId: String): ByteArray? {
        cleanupExpiredChallenges()
        val challenge = challenges[challengeId] ?: return null
        if (challenge.expiresAt.isBefore(Instant.now())) {
            challenges.remove(challengeId)
            return null
        }

        return renderReadableImage(challenge.answer)
    }


    fun verifyChallenge(challengeId: String, userAnswer: String): Boolean {
        cleanupExpiredChallenges()
        val challenge = challenges.remove(challengeId) ?: return false
        if (challenge.expiresAt.isBefore(Instant.now())) {
            return false
        }

        return challenge.normalizedAnswer == userAnswer.trim().lowercase(Locale.ROOT)
    }

    private fun cleanupExpiredChallenges() {
        val now = Instant.now()
        challenges.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }

    private fun generateAnswer(): String {
        return buildString(ANSWER_LENGTH) {
            repeat(ANSWER_LENGTH) {
                append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
        }
    }

    private fun renderReadableImage(answer: String): ByteArray {
        val width = 240
        val height = 72
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.color = Color(245, 247, 250)
        graphics.fillRect(0, 0, width, height)
        graphics.color = Color(190, 198, 206)
        repeat(4) {
            val y = 12 + random.nextInt(height - 24)
            graphics.drawLine(8, y, width - 8, y + random.nextInt(9) - 4)
        }
        graphics.font = Font(Font.MONOSPACED, Font.BOLD, 32)
        graphics.color = Color(25, 45, 72)
        val startX = 18
        answer.forEachIndexed { index, char ->
            graphics.drawString(char.toString(), startX + index * 36, 48)
        }
        graphics.dispose()
        return ByteArrayOutputStream().use { outputStream ->
            ImageIO.write(image, "png", outputStream)
            outputStream.toByteArray()
        }
    }

    private companion object {
        const val ANSWER_LENGTH = 5
        const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}

data class CaptchaChallengeData(val id: String)

private data class CaptchaChallenge(
    val answer: String,
    val normalizedAnswer: String,
    val expiresAt: Instant
)
