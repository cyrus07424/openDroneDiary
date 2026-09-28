package com.opendronediary.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class FlightLog(
    val id: Int,
    val flightDate: String, // 飛行の年月日
    val takeoffLandingLocation: String? = null, // 離着陸場所 (legacy field, keep for backward compatibility)
    val takeoffLandingTime: String? = null, // 離着陸時刻 (legacy field, keep for backward compatibility)
    val flightDuration: String? = null, // 飛行時間 (legacy field)
    val pilotName: String, // 飛行させた者の氏名 (free text or name from selected pilot)
    val pilotId: Int? = null, // 登録済みパイロットのID (optional, if selected from registered pilots)
    val issuesAndResponses: String? = null, // 不具合やその対応 (optional)
    val userId: Int,
    // New enhanced fields
    val takeoffLocation: String? = null, // 離陸場所
    val landingLocation: String? = null, // 着陸場所
    val takeoffTime: String? = null, // 離陸時刻
    val landingTime: String? = null, // 着陸時刻
    val flightSummary: String? = null, // 飛行概要
    val totalFlightTime: String? = null, // 総飛行時間
    // Coordinate and input type fields
    val takeoffInputType: String = "text", // 離陸場所入力種別 (text or coordinates)
    val landingInputType: String = "text", // 着陸場所入力種別 (text or coordinates)
    val takeoffLatitude: BigDecimal? = null, // 離陸場所緯度
    val takeoffLongitude: BigDecimal? = null, // 離陸場所経度
    val landingLatitude: BigDecimal? = null, // 着陸場所緯度
    val landingLongitude: BigDecimal? = null, // 着陸場所経度
    val aircraftId: Int? = null, // 機体台帳のID
    val flightPurpose: String? = null, // 飛行の目的
    val flightRoute: String? = null, // 飛行の経路
    val specificFlight: String? = null, // 飛行禁止空域・飛行の方法（コードのカンマ区切り）
    val safetyMatters: String? = null, // 飛行の安全に影響のあった事項
    val skillCertificateNumber: String? = null, // 記録時点の技能証明番号
    val permissionNumber: String? = null, // 飛行許可・承認番号（任意）
    val cumulativeFlightMinutes: Int? = null, // 製造後の総飛行時間（分、保存時の写し）
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

