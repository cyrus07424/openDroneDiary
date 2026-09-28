package utils

/**
 * 日常点検記録（様式2）の点検項目。
 * 国交省「無人航空機の飛行日誌の取扱いに関するガイドライン」の項目に合わせている。
 */
object DailyInspectionChecklist {
    const val NORMAL = "normal"
    const val ABNORMAL = "abnormal"

    data class Item(val field: String, val label: String, val hint: String)

    val items: List<Item> = listOf(
        Item("airframeResult", "機体全般", "機器の取付状態、損傷、ゆがみ"),
        Item("propellerResult", "プロペラ", "損傷、ゆがみ、取付"),
        Item("frameResult", "フレーム", "損傷、ゆがみ"),
        Item("communicationResult", "通信系統", "機体と操縦装置の通信"),
        Item("propulsionResult", "推進系統", "モーター又は発動機の作動"),
        Item("powerResult", "電源系統", "機体・操縦装置の電源"),
        Item("automaticControlResult", "自動制御系統", "飛行制御装置の作動"),
        Item("controllerResult", "操縦装置", "スティック、スイッチ、表示"),
        Item("batteryResult", "バッテリー・燃料", "残量と固定状態")
    )

    fun labelOf(value: String?): String = when (value) {
        NORMAL -> "異常なし"
        ABNORMAL -> "異常あり"
        else -> "未記入"
    }

    fun isKnown(value: String?): Boolean = value == NORMAL || value == ABNORMAL

    fun summarize(values: Map<String, String?>, remarks: String?): String {
        val lines = items.mapNotNull { item ->
            val value = values[item.field]
            if (!isKnown(value)) null else "${item.label}: ${labelOf(value)}"
        }
        val remarkText = remarks?.trim().orEmpty()
        return when {
            lines.isEmpty() && remarkText.isEmpty() -> "特記事項なし"
            lines.isEmpty() -> remarkText
            remarkText.isEmpty() -> lines.joinToString(" / ")
            else -> lines.joinToString(" / ") + " / 特記: $remarkText"
        }
    }

    fun abnormalLabels(values: Map<String, String?>): List<String> {
        return items.filter { values[it.field] == ABNORMAL }.map { it.label }
    }
}
