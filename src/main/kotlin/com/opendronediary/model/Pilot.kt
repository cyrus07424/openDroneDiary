package com.opendronediary.model

import java.time.LocalDateTime

data class Pilot(
    val id: Int,
    val name: String, // パイロット氏名
    val userId: Int, // 登録したユーザーのID
    val skillCertificateNumber: String? = null, // 無人航空機操縦者技能証明書番号
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)