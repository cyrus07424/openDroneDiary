package com.opendronediary.model

import java.time.LocalDateTime

data class Pilot(
    val id: Int,
    val name: String, // パイロット氏名
    val userId: Int, // 登録したユーザーのID
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)