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
 * Thin client for OpenAI-compatible chat-completions endpoints and Anthropic Messages endpoints.
 *
 * The request format is chosen from the URL: paths ending in `/messages` use the Anthropic
 * Messages API, everything else uses OpenAI chat completions.
 */
class VisionApi(
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
        endpoint: String = GO_ENDPOINT,
        maxTokens: Int = ANTHROPIC_MAX_TOKENS,
        mime: String = "image/jpeg",
    ): String = withContext(Dispatchers.IO) {
        val anthropic = isAnthropic(endpoint)
        val body = if (anthropic) {
            buildAnthropicBody(model, imagesBase64, mime, maxTokens)
        } else {
            buildOpenAiBody(model, imagesBase64, mime)
        }

        val request = Request.Builder()
            .url(endpoint)
            .header("Content-Type", "application/json")
            .header("User-Agent", userAgent)
            .apply {
                if (anthropic) {
                    // Support both the official Anthropic header and proxies that expect a
                    // bearer token (e.g. ANTHROPIC_AUTH_TOKEN).
                    header("x-api-key", apiKey)
                    header("anthropic-version", ANTHROPIC_VERSION)
                    header("Authorization", "Bearer $apiKey")
                } else {
                    header("Authorization", "Bearer $apiKey")
                    header("x-opencode-session", sessionId)
                }
            }
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code, text)
            if (anthropic) extractAnthropicContent(text) else extractOpenAiContent(text)
        }
    }

    private fun isAnthropic(endpoint: String): Boolean =
        endpoint.substringBefore('?').trimEnd('/').endsWith("/messages", ignoreCase = true)

    private fun buildOpenAiBody(model: String, imagesBase64: List<String>, mime: String): String {
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

        return buildJsonObject {
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
        }.toString()
    }

    private fun buildAnthropicBody(
        model: String,
        imagesBase64: List<String>,
        mime: String,
        maxTokens: Int,
    ): String {
        val content = buildJsonArray {
            addJsonObject {
                put("type", "text")
                put("text", Prompts.userText(imagesBase64.size))
            }
            imagesBase64.forEach { b64 ->
                addJsonObject {
                    put("type", "image")
                    putJsonObject("source") {
                        put("type", "base64")
                        put("media_type", mime)
                        put("data", b64)
                    }
                }
            }
        }

        return buildJsonObject {
            put("model", model)
            put("max_tokens", maxTokens)
            put("temperature", 0.2)
            put("system", Prompts.SYSTEM)
            put("messages", buildJsonArray {
                addJsonObject {
                    put("role", "user")
                    put("content", content)
                }
            })
        }.toString()
    }

    private fun extractOpenAiContent(raw: String): String {
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

    private fun extractAnthropicContent(raw: String): String {
        val root = json.parseToJsonElement(raw).jsonObject
        val content = root["content"]?.jsonArray
            ?: throw IOException("响应缺少 content 字段：${raw.take(300)}")
        val text = content.joinToString("") { part ->
            val obj = part.jsonObject
            // Skip thinking/reasoning blocks; accept text blocks even if `type` is missing.
            if (obj["thinking"] != null) "" else obj["text"]?.jsonPrimitive?.content.orEmpty()
        }
        if (text.isNotBlank()) return text

        val stop = root["stop_reason"]?.jsonPrimitive?.content
        val hasThinking = content.any { it.jsonObject["thinking"] != null }
        val hint = if (hasThinking) {
            "模型只返回了思考内容、没有最终答案（max_tokens 可能被思考耗尽），请调高「最大输出 tokens」"
        } else {
            "响应 content 没有文本"
        }
        throw IOException("$hint（stop_reason=${stop ?: "未知"}）：${raw.take(300)}")
    }

    companion object {
        const val GO_ENDPOINT = "https://opencode.ai/zen/go/v1/chat/completions"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val ANTHROPIC_MAX_TOKENS = 8192
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}

class ApiException(
    val code: Int,
    val responseBody: String,
) : IOException("HTTP $code: ${responseBody.take(500)}")
