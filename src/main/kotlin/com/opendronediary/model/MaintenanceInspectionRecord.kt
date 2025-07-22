package com.opendronediary.model

import java.time.LocalDateTime

data class MaintenanceInspectionRecord(
    val id: Int,
    val inspectionDate: String, // 点検整備の実施の年月日
    val location: String, // 場所
    val inspectorName: String, // 実施者の氏名
    val contentAndReason: String, // 点検・修理・改造・整備の内容・理由
    val userId: Int,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)