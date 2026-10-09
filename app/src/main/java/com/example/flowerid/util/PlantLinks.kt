package com.example.flowerid.util

import java.net.URLEncoder

/** Links to authoritative plant databases, opened in the browser for manual verification. */
object PlantLinks {

    data class Site(val label: String, val url: String)

    /** Builds one link per site. iPlant info pages accept the plant name directly. */
    fun sites(name: String, scientificName: String): List<Site> {
        val display = name.trim()
        val key = display.ifBlank { scientificName.trim() }
        if (key.isEmpty()) return emptyList()
        val enc = encode(key)
        return listOf(
            Site("中国植物志 · iPlant", "https://www.iplant.cn/info/$enc"),
            Site("中国植物图像库", "https://www.bing.com/search?q=site%3Appbc.iplant.cn+$enc"),
            Site("百度百科", "https://baike.baidu.com/item/$enc"),
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
