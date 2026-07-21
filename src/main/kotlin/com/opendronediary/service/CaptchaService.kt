package com.opendronediary.service

import com.github.cage.Cage
import com.github.cage.GCage
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

class CaptchaService(
    private val cage: Cage = GCage(),
    private val challengeTtl: Duration = Duration.ofMinutes(10)
) {
    private val challenges = ConcurrentHashMap<String, CaptchaChallenge>()

    fun createChallenge(): CaptchaChallengeData {
        cleanupExpiredChallenges()
        val answer = cage.tokenGenerator.next()
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

        return ByteArrayOutputStream().use { outputStream ->
            ImageIO.write(cage.drawImage(challenge.answer), "png", outputStream)
            outputStream.toByteArray()
        }
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
}

data class CaptchaChallengeData(val id: String)

private data class CaptchaChallenge(
    val answer: String,
    val normalizedAnswer: String,
    val expiresAt: Instant
)
