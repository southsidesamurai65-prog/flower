package com.example.flowerid.di

import android.content.Context
import androidx.room.Room
import com.example.flowerid.data.api.VisionApi
import com.example.flowerid.data.local.AppDatabase
import com.example.flowerid.data.repo.HistoryRepository
import com.example.flowerid.data.repo.IdentifyRepository
import com.example.flowerid.security.ApiKeyStore

/** Simple manual dependency container (no DI framework). */
class AppContainer(context: Context) {

    val apiKeyStore: ApiKeyStore = ApiKeyStore(context)

    private val visionApi: VisionApi = VisionApi()

    private val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "flowerid.db",
    ).fallbackToDestructiveMigration().build()

    val historyRepository: HistoryRepository = HistoryRepository(database.historyDao())

    val identifyRepository: IdentifyRepository = IdentifyRepository(
        context = context,
        api = visionApi,
        keyStore = apiKeyStore,
        historyDao = database.historyDao(),
    )
}
