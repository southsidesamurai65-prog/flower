package com.example.flowerid.data.repo

import android.content.Context
import android.net.Uri
import com.example.flowerid.data.api.ApiException
import com.example.flowerid.data.api.VisionApi
import com.example.flowerid.data.local.HistoryDao
import com.example.flowerid.data.local.HistoryItem
import com.example.flowerid.data.model.Candidate
import com.example.flowerid.data.model.IdentifyPayload
import com.example.flowerid.data.model.IdentifyResult
import com.example.flowerid.data.model.MissingApiKeyException
import com.example.flowerid.data.model.PlantTags
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

    /** Runs identification only; the result is not persisted until the user archives a candidate. */
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
        IdentifyResult(
            payload = payload,
            modelUsed = modelUsed,
            elapsedMs = System.currentTimeMillis() - start,
        )
    }

    /** Persists the user-chosen candidate (with its normalized tags) as an archived history entry. */
    suspend fun archive(
        firstImage: Uri,
        payload: IdentifyPayload,
        candidate: Candidate,
        modelUsed: String,
    ): Long = withContext(Dispatchers.IO) {
        val createdAt = System.currentTimeMillis()
        val thumb = writeThumbnail(firstImage, createdAt)
        historyDao.insert(
            HistoryItem(
                createdAt = createdAt,
                thumbPath = thumb,
                title = candidate.name.ifBlank { "未识别" },
                scientificName = candidate.scientificName,
                category = PlantTags.normalizeFamily(candidate.family),
                leafForm = PlantTags.normalize(candidate.tags.leafForm, PlantTags.LEAF_FORMS),
                leafShape = PlantTags.normalize(candidate.tags.leafShape, PlantTags.LEAF_SHAPES),
                leafArrangement = PlantTags.normalize(candidate.tags.leafArrangement, PlantTags.LEAF_ARRANGEMENTS),
                leafMargin = PlantTags.normalize(candidate.tags.leafMargin, PlantTags.LEAF_MARGINS),
                flowerShape = PlantTags.normalize(candidate.tags.flowerShape, PlantTags.FLOWER_SHAPES),
                inflorescence = PlantTags.normalize(candidate.tags.inflorescence, PlantTags.INFLORESCENCES),
                ovaryPosition = PlantTags.normalize(candidate.tags.ovaryPosition, PlantTags.OVARY_POSITIONS),
                fruitType = PlantTags.normalize(candidate.tags.fruitType, PlantTags.FRUIT_TYPES),
                resultJson = PayloadParser.toJson(payload),
                modelUsed = modelUsed,
            ),
        )
    }

    private fun writeThumbnail(uri: Uri, createdAt: Long): String {
        val dir = File(context.filesDir, "thumbs").apply { mkdirs() }
        val file = File(dir, "thumb_$createdAt.jpg")
        val bytes = ImageUtils.toJpegBytes(context, uri, maxEdge = 320, quality = 70)
        file.writeBytes(bytes)
        return file.absolutePath
    }
}
