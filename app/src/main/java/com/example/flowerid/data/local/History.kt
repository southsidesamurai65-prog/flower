package com.example.flowerid.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val thumbPath: String,
    val title: String,
    val scientificName: String = "",
    val category: String = "",
    val leafForm: String = "",
    val leafShape: String = "",
    val leafArrangement: String = "",
    val leafMargin: String = "",
    val flowerShape: String = "",
    val inflorescence: String = "",
    val ovaryPosition: String = "",
    val fruitType: String = "",
    val resultJson: String,
    val modelUsed: String,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryItem>>

    @Query(
        """
        SELECT * FROM history
        WHERE (:category IS NULL OR category = :category)
          AND (:leafForm IS NULL OR leafForm = :leafForm)
          AND (:leafShape IS NULL OR leafShape = :leafShape)
          AND (:leafArrangement IS NULL OR leafArrangement = :leafArrangement)
          AND (:leafMargin IS NULL OR leafMargin = :leafMargin)
          AND (:flowerShape IS NULL OR flowerShape = :flowerShape)
          AND (:inflorescence IS NULL OR inflorescence = :inflorescence)
          AND (:ovaryPosition IS NULL OR ovaryPosition = :ovaryPosition)
          AND (:fruitType IS NULL OR fruitType = :fruitType)
        ORDER BY createdAt DESC
        """,
    )
    fun observeFiltered(
        category: String?,
        leafForm: String?,
        leafShape: String?,
        leafArrangement: String?,
        leafMargin: String?,
        flowerShape: String?,
        inflorescence: String?,
        ovaryPosition: String?,
        fruitType: String?,
    ): Flow<List<HistoryItem>>

    @Query("SELECT DISTINCT category FROM history WHERE category != '' ORDER BY category")
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): HistoryItem?

    @Insert
    suspend fun insert(item: HistoryItem): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [HistoryItem::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
