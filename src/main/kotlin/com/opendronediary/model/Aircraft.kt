package com.opendronediary.model

import java.time.LocalDateTime

/**
 * 機体台帳。航空法の飛行日誌は機体ごとに作成する。
 * 登録記号・型式・製造番号・認証番号は様式の冒頭記載事項。
 */
data class Aircraft(
    val id: Int,
    val registrationSymbol: String,
    val userId: Int,
    val manufacturer: String? = null,
    val modelName: String? = null,
    val serialNumber: String? = null,
    val category: String? = null,
    val typeCertificateNumber: String? = null,
    val aircraftCertificateClass: String? = null,
    val aircraftCertificateNumber: String? = null,
    /** このシステム導入前に引き継いだ製造後の総飛行時間（分） */
    val initialTotalMinutes: Int = 0,
    /** メーカー指定がなければ国交省目安の20時間 */
    val maintenanceIntervalHours: Int = 20,
    val notes: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    fun displayName(): String {
        val type = listOfNotNull(manufacturer, modelName).joinToString(" ").ifBlank { "型式未設定" }
        return "$registrationSymbol（$type）"
    }
}
