package com.example.flowerid.data.repo

import com.example.flowerid.data.local.HistoryDao
import com.example.flowerid.data.local.HistoryItem
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {

    fun observeAll(): Flow<List<HistoryItem>> = dao.observeAll()

    suspend fun get(id: Long): HistoryItem? = dao.get(id)

    suspend fun delete(id: Long) = dao.delete(id)
}
