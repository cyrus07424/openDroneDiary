package com.opendronediary.model

import java.time.LocalDateTime

data class DailyInspectionRecord(
    val id: Int,
    val inspectionDate: String, // 日常点検の実施の年月日
    val location: String, // 場所
    val inspectorName: String, // 実施者の氏名
    val inspectionResult: String, // 日常点検の結果
    val userId: Int,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)