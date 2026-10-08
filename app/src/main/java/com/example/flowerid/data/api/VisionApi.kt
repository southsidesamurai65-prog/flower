package com.example.flowerid.data.api

import com.example.flowerid.prompt.Prompts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Thin client for the OpenCode Go (OpenAI-compatible) chat completions endpoint.
 *
 * Endpoint: https://opencode.ai/zen/go/v1/chat/completions
 * Model:    deepseek-v4.1-flash (vision capable)
 */
class VisionApi(
    private val endpoint: String = GO_ENDPOINT,
    private val userAgent: String = "FlowerID/1.0",
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Stable session id helps the gateway with routing / prompt caching.
    private val sessionId: String = UUID.randomUUID().toString()

    /** Returns the raw assistant message content (a JSON string) for the given base64 JPEG images. */
    suspend fun identify(
        apiKey: String,
        model: String,
        imagesBase64: List<String>,
        mime: String = "image/jpeg",
    ): String = withContext(Dispatchers.IO) {
        val content = buildJsonArray {
            addJsonObject {
                put("type", "text")
                put("text", Prompts.userText(imagesBase64.size))
            }
            imagesBase64.forEach { b64 ->
                addJsonObject {
                    put("type", "image_url")
                    putJsonObject("image_url") {
                        put("url", "data:$mime;base64,$b64")
                        put("detail", "original")
                    }
                }
            }
        }

        val body = buildJsonObject {
            put("model", model)
            put("temperature", 0.2)
            put("stream", false)
            putJsonObject("response_format") { put("type", "json_object") }
            put("messages", buildJsonArray {
                addJsonObject {
                    put("role", "system")
                    put("content", Prompts.SYSTEM)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", content)
                }
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("x-opencode-session", sessionId)
            .header("User-Agent", userAgent)
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code, text)
            extractContent(text)
        }
    }

    private fun extractContent(raw: String): String {
        val root = json.parseToJsonElement(raw).jsonObject
        val choices = root["choices"]?.jsonArray
            ?: throw IOException("响应缺少 choices 字段：${raw.take(300)}")
        val message = choices.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            ?: throw IOException("响应缺少 message 字段")
        val contentEl = message["content"] ?: throw IOException("响应缺少 content 字段")
        return when {
            contentEl is kotlinx.serialization.json.JsonArray ->
                contentEl.joinToString("") { part ->
                    part.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
                }
            else -> contentEl.jsonPrimitive.content
        }
    }

    companion object {
        const val GO_ENDPOINT = "https://opencode.ai/zen/go/v1/chat/completions"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}

class ApiException(
    val code: Int,
    val responseBody: String,
) : IOException("HTTP $code: ${responseBody.take(500)}")
