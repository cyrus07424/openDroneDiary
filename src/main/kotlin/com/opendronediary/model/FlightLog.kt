package com.opendronediary.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class FlightLog(
    val id: Int,
    val flightDate: String, // 飛行の年月日
    val takeoffLandingLocation: String? = null, // 離着陸場所 (legacy field, keep for backward compatibility)
    val takeoffLandingTime: String? = null, // 離着陸時刻 (legacy field, keep for backward compatibility)
    val flightDuration: String? = null, // 飛行時間 (legacy field)
    val pilotName: String, // 飛行させた者の氏名
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
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

