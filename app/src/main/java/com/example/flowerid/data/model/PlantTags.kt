package com.example.flowerid.data.model

/**
 * Fixed, orthogonal tag vocabularies shared by the model prompt, the archive dialog and the
 * history filters. Family (科) is intentionally not enumerated: it is taken from existing data.
 */
object PlantTags {

    val LEAF_FORMS = listOf("单叶", "羽状复叶", "掌状复叶", "三出复叶", "单身复叶", OTHER)
    val LEAF_SHAPES = listOf(
        "针形", "条形", "披针形", "卵形", "椭圆形", "长圆形", "圆形", "心形", "肾形",
        "菱形", "三角形", "箭形", "戟形", "匙形", "扇形", "鳞片形", OTHER,
    )
    val LEAF_ARRANGEMENTS = listOf("互生", "对生", "轮生", "簇生", "基生", OTHER)
    val LEAF_MARGINS = listOf("全缘", "锯齿", "齿牙", "重锯齿", "波状", "睫毛状", OTHER)

    val FLOWER_SHAPES = listOf(
        "钟形", "漏斗形", "坛形", "管状", "舌状", "唇形", "蝶形", "辐状(轮状)",
        "高脚碟形", "十字形", "球状", OTHER,
    )
    val INFLORESCENCES = listOf(
        "单生", "总状花序", "穗状花序", "伞房花序", "伞形花序", "头状花序", "圆锥花序",
        "聚伞花序", "柔荑花序", "隐头花序", "轮伞花序", OTHER,
    )
    val OVARY_POSITIONS = listOf("子房上位(下位花)", "子房下位(上位花)", "子房半下位(周位花)", OTHER)

    val FRUIT_TYPES = listOf(
        "蓇葖果", "荚果", "蒴果", "角果", "瘦果", "颖果", "翅果", "坚果", "小坚果",
        "双悬果", "浆果", "核果", "柑果", "瓠果", "梨果", "聚合果", "聚花果", "球果", OTHER,
    )

    const val OTHER = "其他"

    /** Keep only the Chinese family name, dropping Latin/parenthetical parts: "蔷薇科 Rosaceae" -> "蔷薇科". */
    fun normalizeFamily(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val cut = trimmed.indexOfFirst { it == ' ' || it == '(' || it == '（' || it == '/' }
        return (if (cut >= 0) trimmed.substring(0, cut) else trimmed).trim()
    }

    /** Snap a model-provided value onto [allowed], tolerating a missing parenthetical suffix. */
    fun normalize(value: String, allowed: List<String>): String {
        val v = value.trim()
        if (v.isEmpty()) return ""
        if (allowed.contains(v)) return v
        allowed.firstOrNull { a ->
            val base = a.substringBefore('(')
            base == v || v.startsWith(base) || base.startsWith(v)
        }?.let { return it }
        return OTHER
    }
}
