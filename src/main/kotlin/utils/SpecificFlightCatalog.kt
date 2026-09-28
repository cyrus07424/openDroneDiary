package utils

/**
 * 飛行記録の「飛行させた飛行禁止空域及び飛行の方法」。
 * 航空法の特定飛行の区分に合わせた選択項目。
 */
object SpecificFlightCatalog {
    data class Category(val code: String, val label: String, val group: String)

    val all: List<Category> = listOf(
        Category("airport", "空港等の周辺", "飛行禁止空域"),
        Category("emergency", "緊急用務空域", "飛行禁止空域"),
        Category("above150m", "地表又は水面から150m以上", "飛行禁止空域"),
        Category("did", "人口集中地区の上空", "飛行禁止空域"),
        Category("night", "夜間飛行", "飛行の方法"),
        Category("bvlos", "目視外飛行", "飛行の方法"),
        Category("within30m", "人又は物件から30m未満", "飛行の方法"),
        Category("event", "催し場所の上空", "飛行の方法"),
        Category("dangerous", "危険物の輸送", "飛行の方法"),
        Category("drop", "物件の投下", "飛行の方法"),
        Category("none", "特定飛行に該当しない", "区分")
    )

    private val byCode = all.associateBy { it.code }

    fun parse(stored: String?): Set<String> {
        if (stored.isNullOrBlank()) return emptySet()
        return stored.split(',')
            .map { it.trim() }
            .filter { it in byCode }
            .toSet()
    }

    fun store(codes: Collection<String>?): String? {
        if (codes.isNullOrEmpty()) return null
        val known = codes.map { it.trim() }.filter { it in byCode }.distinct()
        if (known.isEmpty()) return null
        val specific = known.filter { it != "none" }
        val normalized = if (specific.isNotEmpty()) specific else listOf("none")
        return normalized.joinToString(",")
    }

    fun format(stored: String?): String {
        val codes = parse(stored)
        if (codes.isEmpty()) return ""
        return all.filter { it.code in codes }.joinToString("、") { it.label }
    }

    fun isSpecificFlight(stored: String?): Boolean {
        val codes = parse(stored)
        return codes.isNotEmpty() && codes != setOf("none")
    }
}
