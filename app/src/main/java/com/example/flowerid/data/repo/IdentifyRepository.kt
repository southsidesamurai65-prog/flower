package com.example.flowerid.data.repo

import android.content.Context
import android.net.Uri
import com.example.flowerid.data.api.ApiException
import com.example.flowerid.data.api.VisionApi
import com.example.flowerid.data.local.HistoryDao
import com.example.flowerid.data.local.HistoryItem
import com.example.flowerid.data.model.IdentifyPayload
import com.example.flowerid.data.model.IdentifyResult
import com.example.flowerid.data.model.MissingApiKeyException
import com.example.flowerid.security.ApiKeyStore
import com.example.flowerid.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class IdentifyRepository(
    private val context: Context,
    private val api: VisionApi,
    private val keyStore: ApiKeyStore,
    private val historyDao: HistoryDao,
) {

    suspend fun identify(uris: List<Uri>): IdentifyResult = withContext(Dispatchers.IO) {
        require(uris.isNotEmpty()) { "请先拍摄或选择至少一张照片" }
        val apiKey = keyStore.getApiKey() ?: throw MissingApiKeyException()

        val maxEdge = keyStore.getMaxEdge()
        val quality = keyStore.getQuality()
        val images = uris.map { ImageUtils.toJpegBase64(context, it, maxEdge, quality) }

        val primaryModel = keyStore.getModel()
        val start = System.currentTimeMillis()

        val (raw, modelUsed) = try {
            api.identify(apiKey, primaryModel, images) to primaryModel
        } catch (e: ApiException) {
            val canFallback = primaryModel != ApiKeyStore.FALLBACK_MODEL &&
                (e.code == 400 || e.code == 404 || e.code == 422)
            if (canFallback) {
                api.identify(apiKey, ApiKeyStore.FALLBACK_MODEL, images) to ApiKeyStore.FALLBACK_MODEL
            } else {
                throw e
            }
        }

        val payload = PayloadParser.parse(raw)
        val result = IdentifyResult(
            payload = payload,
            modelUsed = modelUsed,
            elapsedMs = System.currentTimeMillis() - start,
        )
        saveHistory(uris.first(), payload, result)
        result
    }

    private suspend fun saveHistory(first: Uri, payload: IdentifyPayload, result: IdentifyResult) {
        runCatching {
            val createdAt = System.currentTimeMillis()
            val thumb = writeThumbnail(first, createdAt)
            historyDao.insert(
                HistoryItem(
                    createdAt = createdAt,
                    thumbPath = thumb,
                    title = payload.candidates.firstOrNull()?.name?.ifBlank { "未识别" } ?: "未识别",
                    resultJson = PayloadParser.toJson(payload),
                    modelUsed = result.modelUsed,
                ),
            )
        }
    }

    private fun writeThumbnail(uri: Uri, createdAt: Long): String {
        val dir = File(context.filesDir, "thumbs").apply { mkdirs() }
        val file = File(dir, "thumb_$createdAt.jpg")
        val bytes = ImageUtils.toJpegBytes(context, uri, maxEdge = 320, quality = 70)
        file.writeBytes(bytes)
        return file.absolutePath
    }
}
