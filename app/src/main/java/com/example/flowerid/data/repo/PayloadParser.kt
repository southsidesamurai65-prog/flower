package com.example.flowerid.data.repo

import com.example.flowerid.data.model.IdentifyPayload
import kotlinx.serialization.json.Json

/** Parses the model's text response into an [IdentifyPayload], tolerating stray prose/fences. */
object PayloadParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun parse(raw: String): IdentifyPayload {
        val cleaned = extractJsonObject(raw)
        return json.decodeFromString(IdentifyPayload.serializer(), cleaned)
    }

    fun toJson(payload: IdentifyPayload): String =
        json.encodeToString(IdentifyPayload.serializer(), payload)

    fun fromJson(value: String): IdentifyPayload =
        json.decodeFromString(IdentifyPayload.serializer(), value)

    private fun extractJsonObject(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```json").removePrefix("```").trim()
            val fenceEnd = text.lastIndexOf("```")
            if (fenceEnd >= 0) text = text.substring(0, fenceEnd).trim()
        }
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start in 0 until end) text.substring(start, end + 1) else text
    }
}
