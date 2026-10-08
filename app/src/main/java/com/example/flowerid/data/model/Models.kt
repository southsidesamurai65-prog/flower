package com.example.flowerid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Confusable(
    val name: String = "",
    val difference: String = "",
)

@Serializable
data class Candidate(
    val name: String = "",
    @SerialName("scientific_name") val scientificName: String = "",
    val confidence: Double = 0.0,
    val family: String = "",
    val genus: String = "",
    val aliases: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    @SerialName("confusable_with") val confusableWith: List<Confusable> = emptyList(),
    val reasoning: String = "",
)

@Serializable
data class IdentifyPayload(
    val candidates: List<Candidate> = emptyList(),
    val note: String = "",
)

/** Options resolved from user settings at request time. */
data class IdentifyOptions(
    val model: String,
    val quality: Int,
    val maxEdge: Int,
)

data class IdentifyResult(
    val payload: IdentifyPayload,
    val modelUsed: String,
    val elapsedMs: Long,
)

class MissingApiKeyException : Exception("尚未配置 API Key")
