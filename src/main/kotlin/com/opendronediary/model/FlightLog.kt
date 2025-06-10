package com.opendronediary.model

data class FlightLog(
    val id: Int,
    val flightDate: String, // 飛行の年月日
    val takeoffLandingLocation: String, // 離着陸場所
    val takeoffLandingTime: String, // 離着陸時刻
    val flightDuration: String, // 飛行時間
    val pilotName: String, // 飛行させた者の氏名
    val issuesAndResponses: String? = null, // 不具合やその対応 (optional)
    val userId: Int
)

