package com.example.flowerid.data.repo

import com.example.flowerid.data.local.HistoryDao
import com.example.flowerid.data.local.HistoryItem
import kotlinx.coroutines.flow.Flow

/** Selected tag filters; a null/blank value means "don't filter on this dimension". */
data class HistoryFilter(
    val category: String? = null,
    val leafForm: String? = null,
    val leafShape: String? = null,
    val leafArrangement: String? = null,
    val leafMargin: String? = null,
    val flowerShape: String? = null,
    val inflorescence: String? = null,
    val ovaryPosition: String? = null,
    val fruitType: String? = null,
) {
    val isEmpty: Boolean
        get() = listOf(
            category, leafForm, leafShape, leafArrangement, leafMargin,
            flowerShape, inflorescence, ovaryPosition, fruitType,
        ).all { it.isNullOrBlank() }
}

class HistoryRepository(private val dao: HistoryDao) {

    fun observeAll(): Flow<List<HistoryItem>> = dao.observeAll()

    fun observeFiltered(filter: HistoryFilter): Flow<List<HistoryItem>> = dao.observeFiltered(
        category = filter.category.nullIfBlank(),
        leafForm = filter.leafForm.nullIfBlank(),
        leafShape = filter.leafShape.nullIfBlank(),
        leafArrangement = filter.leafArrangement.nullIfBlank(),
        leafMargin = filter.leafMargin.nullIfBlank(),
        flowerShape = filter.flowerShape.nullIfBlank(),
        inflorescence = filter.inflorescence.nullIfBlank(),
        ovaryPosition = filter.ovaryPosition.nullIfBlank(),
        fruitType = filter.fruitType.nullIfBlank(),
    )

    fun observeCategories(): Flow<List<String>> = dao.observeCategories()

    suspend fun get(id: Long): HistoryItem? = dao.get(id)

    suspend fun delete(id: Long) = dao.delete(id)

    private fun String?.nullIfBlank(): String? = if (isNullOrBlank()) null else this
}
