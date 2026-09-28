package com.opendronediary.model

import java.time.LocalDateTime

data class DailyInspectionRecord(
    val id: Int,
    val inspectionDate: String, // 日常点検の実施の年月日
    val location: String, // 場所
    val inspectorName: String, // 実施者の氏名
    val inspectionResult: String, // 日常点検の結果（項目要約または自由記述）
    val userId: Int,
    val aircraftId: Int? = null,
    val airframeResult: String? = null,
    val propellerResult: String? = null,
    val frameResult: String? = null,
    val communicationResult: String? = null,
    val propulsionResult: String? = null,
    val powerResult: String? = null,
    val automaticControlResult: String? = null,
    val controllerResult: String? = null,
    val batteryResult: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    fun checklistValues(): Map<String, String?> = mapOf(
        "airframeResult" to airframeResult,
        "propellerResult" to propellerResult,
        "frameResult" to frameResult,
        "communicationResult" to communicationResult,
        "propulsionResult" to propulsionResult,
        "powerResult" to powerResult,
        "automaticControlResult" to automaticControlResult,
        "controllerResult" to controllerResult,
        "batteryResult" to batteryResult
    )
}