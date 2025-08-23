package com.opendronediary.service

import com.nulabinc.zxcvbn.Zxcvbn

/**
 * Service for evaluating password strength using zxcvbn library
 */
class PasswordStrengthService {
    private val zxcvbn = Zxcvbn()
    
    /**
     * Validates if a password meets minimum strength requirements
     * @param password The password to validate
     * @param userInputs Optional list of user inputs (username, email) to check against
     * @return PasswordValidationResult containing validation status and feedback
     */
    fun validatePassword(password: String, userInputs: List<String> = emptyList()): PasswordValidationResult {
        val strength = zxcvbn.measure(password, userInputs)
        
        // Require score of at least 3 (out of 4) for strong passwords
        val isValid = strength.score >= 3
        
        val feedback = when (strength.score) {
            0 -> "パスワードが非常に弱いです"
            1 -> "パスワードが弱いです"
            2 -> "パスワードの強度が不十分です"
            3 -> "パスワードの強度は良好です"
            4 -> "パスワードの強度は非常に良好です"
            else -> "パスワードを評価できませんでした"
        }
        
        val suggestions = strength.feedback?.suggestions?.joinToString("、") ?: ""
        val warning = strength.feedback?.warning ?: ""
        
        return PasswordValidationResult(
            isValid = isValid,
            score = strength.score,
            feedback = feedback,
            suggestions = suggestions,
            warning = warning,
            crackTimeDisplay = strength.crackTimesDisplay?.toString() ?: "不明"
        )
    }
}

/**
 * Result of password strength validation
 */
data class PasswordValidationResult(
    val isValid: Boolean,
    val score: Int, // 0-4, with 4 being strongest
    val feedback: String,
    val suggestions: String,
    val warning: String,
    val crackTimeDisplay: String
)